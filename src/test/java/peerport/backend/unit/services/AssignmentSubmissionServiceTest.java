package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import peerport.backend.database.AssignmentSubmissionRepository;
import peerport.backend.exceptions.assignmentSubmissions.AssignmentSubmissionNotFoundException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.AssignmentModel;
import peerport.backend.model.AssignmentSubmissionModel;
import peerport.backend.model.CourseModel;
import peerport.backend.model.EnrollmentModel;
import peerport.backend.model.FileModel;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.model.UserModel;
import peerport.backend.service.AssignmentService;
import peerport.backend.service.AssignmentSubmissionService;
import peerport.backend.service.AuthService;
import peerport.backend.service.FileService;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentSubmissionService Unit Tests")
class AssignmentSubmissionServiceTest {

    @InjectMocks
    private AssignmentSubmissionService service;

    @Mock
    private AssignmentSubmissionRepository repository;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private AuthService authService;

    @Mock
    private FileService fileService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "fileUploadSizeLimit", 5_000_000L);
    }

    @Nested
    class GetAllTests {
        @Test
        void admin_getsAllFromRepository() {
            UserModel admin = new UserModel("admin", "Admin", "a@test.com", null, null, Role.ADMIN);
            when(authService.getCurrentUser()).thenReturn(admin);
            when(repository.findAll()).thenReturn(List.of(new AssignmentSubmissionModel()));

            List<AssignmentSubmissionModel> result = service.getAllAssignmentSubmissions();

            assertEquals(1, result.size());
            verify(repository).findAll();
        }

        @Test
        void instructor_getsUniqueSubmissionsFromTaughtCourses() {
            UserModel instructor = new UserModel("i1", "Inst", "i@test.com", null, null, Role.INSTRUCTOR);
            CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
            AssignmentModel assignment = new AssignmentModel("a1", "A1", "desc", true, new Date(), new Date(), new Date(), course);

            AssignmentSubmissionModel s1 = new AssignmentSubmissionModel("s1", new Date(), "c", instructor, assignment, List.of());
            AssignmentSubmissionModel duplicateS1 = new AssignmentSubmissionModel("s1", new Date(), "c2", instructor, assignment, List.of());
            assignment.setSubmissions(List.of(s1, duplicateS1));
            course.addAssignment(assignment);
            instructor.addTaughtCourse(course);

            when(authService.getCurrentUser()).thenReturn(instructor);

            List<AssignmentSubmissionModel> result = service.getAllAssignmentSubmissions();

            assertEquals(1, result.size());
            assertEquals("s1", result.get(0).getAssignmentSubmissionId());
            verify(repository, never()).findAll();
        }

        @Test
        void student_getsOnlyOwnSubmissions() {
            UserModel student = new UserModel("u1", "Stu", "s@test.com", null, null, Role.STUDENT);
            UserModel other = new UserModel("u2", "Other", "o@test.com", null, null, Role.STUDENT);

            CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
            AssignmentModel assignment = new AssignmentModel("a1", "A1", "desc", true, new Date(), new Date(), new Date(), course);

            AssignmentSubmissionModel own = new AssignmentSubmissionModel("s1", new Date(), "mine", student, assignment, List.of());
            AssignmentSubmissionModel notOwn = new AssignmentSubmissionModel("s2", new Date(), "other", other, assignment, List.of());
            assignment.setSubmissions(List.of(own, notOwn));

            EnrollmentModel enrollment = new EnrollmentModel("e1", null, student, course);
            student.addEnrollment(enrollment);
            course.addAssignment(assignment);

            when(authService.getCurrentUser()).thenReturn(student);

            List<AssignmentSubmissionModel> result = service.getAllAssignmentSubmissions();

            assertEquals(1, result.size());
            assertEquals("s1", result.get(0).getAssignmentSubmissionId());
        }
    }

    @Nested
    class GetByIdTests {
        @Test
        void getSubmissionById_whenMissing_throws() {
            when(repository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(AssignmentSubmissionNotFoundException.class, () -> service.getSubmissionById("missing"));
        }

        @Test
        void getSubmissionById_whenUnauthorizedStudent_throws() {
            UserModel owner = new UserModel("owner", "Owner", "owner@test.com", null, null, Role.STUDENT);
            UserModel requester = new UserModel("requester", "Requester", "req@test.com", null, null, Role.STUDENT);
            CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
            AssignmentModel assignment = new AssignmentModel("a1", "A1", "desc", true, new Date(), new Date(), new Date(), course);
            AssignmentSubmissionModel submission = new AssignmentSubmissionModel("s1", new Date(), "x", owner, assignment, List.of());

            when(repository.findById("s1")).thenReturn(Optional.of(submission));
            when(authService.getCurrentUser()).thenReturn(requester);

            assertThrows(UserNotAuthorizedException.class, () -> service.getSubmissionById("s1"));
        }
    }

    @Nested
    class CreateAndDeleteTests {
        @Test
        void createAssignmentSubmission_linksUserAndAssignment() {
            UserModel student = new UserModel("u1", "Stu", "s@test.com", null, null, Role.STUDENT);
            CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
            AssignmentModel assignment = new AssignmentModel("a1", "A1", "desc", true, new Date(), new Date(), new Date(), course);

            AssignmentSubmissionModel request = new AssignmentSubmissionModel();
            request.setUser(student);

            when(authService.getCurrentUser()).thenReturn(student);
            when(repository.save(request)).thenReturn(request);

            AssignmentSubmissionModel result = service.createAssignmentSubmission(request, assignment);

            assertEquals(student, result.getUser());
            assertEquals(assignment, result.getAssignment());
            verify(repository).save(request);
        }

        @Test
        void createAssignmentSubmission_withFiles_savesFilesAndSubmission() throws IOException {
            UserModel student = new UserModel("u1", "Stu", "s@test.com", null, null, Role.STUDENT);
            CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
            AssignmentModel assignment = new AssignmentModel("a1", "A1", "desc", true, new Date(), new Date(), new Date(), course);

            AssignmentSubmissionModel request = new AssignmentSubmissionModel();
            AssignmentSubmissionModel saved = new AssignmentSubmissionModel("s1", new Date(), "ok", student, assignment, List.of());
            MockMultipartFile file = new MockMultipartFile("files", "a.txt", "text/plain", "abc".getBytes());
            FileModel savedFile = new FileModel("a.txt", "tmp/a.txt", "txt", "text/plain");

            when(assignmentService.getAssignmentById("a1")).thenReturn(assignment);
            when(authService.getCurrentUser()).thenReturn(student);
            when(repository.save(any(AssignmentSubmissionModel.class))).thenReturn(saved, saved);
            when(fileService.saveAssignmentSubmissionFiles(List.of(file), assignment, saved)).thenReturn(List.of(savedFile));

            AssignmentSubmissionModel result = service.createAssignmentSubmission(request, "a1", List.of(file));

            assertTrue(result.getSubmittedFiles().size() == 1);
            verify(fileService).checkFileSizes(List.of(file));
            verify(repository, times(2)).save(any(AssignmentSubmissionModel.class));
        }

        @Test
        void createAssignmentSubmission_withNullFiles_doesNotSaveFiles() throws IOException {
            UserModel student = new UserModel("u1", "Stu", "s@test.com", null, null, Role.STUDENT);
            CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
            AssignmentModel assignment = new AssignmentModel("a1", "A1", "desc", true, new Date(), new Date(), new Date(), course);

            AssignmentSubmissionModel request = new AssignmentSubmissionModel();
            AssignmentSubmissionModel saved = new AssignmentSubmissionModel("s1", new Date(), "ok", student, assignment, List.of());

            when(assignmentService.getAssignmentById("a1")).thenReturn(assignment);
            when(authService.getCurrentUser()).thenReturn(student);
            when(repository.save(any(AssignmentSubmissionModel.class))).thenReturn(saved);

            AssignmentSubmissionModel result = service.createAssignmentSubmission(request, "a1", null);

            assertEquals(saved, result);
            verify(fileService).checkFileSizes(null);
            verify(fileService, never()).saveAssignmentSubmissionFiles(any(), any(), any());
            verify(repository, times(1)).save(any(AssignmentSubmissionModel.class));
        }

        @Test
        void deleteAssignmentSubmissionById_deletesWhenFoundAndAuthorized() {
            UserModel student = new UserModel("u1", "Stu", "s@test.com", null, null, Role.STUDENT);
            CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
            AssignmentModel assignment = new AssignmentModel("a1", "A1", "desc", true, new Date(), new Date(), new Date(), course);
            AssignmentSubmissionModel submission = new AssignmentSubmissionModel("s1", new Date(), "ok", student, assignment, List.of());

            when(repository.findById("s1")).thenReturn(Optional.of(submission));
            when(authService.getCurrentUser()).thenReturn(student);

            service.deleteAssignmentSubmissionById("s1");

            verify(repository).deleteById("s1");
        }
    }
}
