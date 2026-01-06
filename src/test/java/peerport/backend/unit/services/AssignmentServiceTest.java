package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import peerport.backend.database.AssignmentsRepository;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.exceptions.assignments.AssignmentNotFoundException;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.AssignmentModel;
import peerport.backend.model.CourseModel;
import peerport.backend.model.FileModel;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.model.UserModel;
import peerport.backend.service.AssignmentService;
import peerport.backend.service.AuthService;
import peerport.backend.service.CourseService;
import peerport.backend.service.FileService;

@ExtendWith(MockitoExtension.class)
public class AssignmentServiceTest {
    
    @InjectMocks
    private AssignmentService assignmentService;

    @Mock
    private AuthService authService;

    @Mock
    private AssignmentsRepository assignmentRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private FileService fileService;

    private static UserModel adminUser, instructorUser, studentUser; 

    @BeforeAll
    static void initAll() {
        makeUsers();
    }

    static void makeUsers() {
        adminUser = new UserModel("admin", "admin", "admin", null, null, Role.ADMIN);
        instructorUser = new UserModel("instructor", "instructor", "instructor", null, null, Role.INSTRUCTOR);
        studentUser = new UserModel("student", "student", "student", null, null, Role.STUDENT);
    }

    @BeforeEach
    void setup() {
        makeUsers();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        ReflectionTestUtils.setField(assignmentService, "validator", validator);
        ReflectionTestUtils.setField(assignmentService, "fileUploadSizeLimit", DEFAULT_SIZE_LIMIT);
    }

    private Validator validator;

    private static final long DEFAULT_SIZE_LIMIT = 5_000_000L;

    @Nested
    @DisplayName("validateAssignment Tests")
    class ValidateAssignmentTests {
        
        private AssignmentModel validAssignment;
        
        @BeforeEach
        void setup() {
            validAssignment = new AssignmentModel();
            validAssignment.setName("Test Assignment");
            validAssignment.setDescription("Test Description");
            validAssignment.setDueDate(new Date());
            validAssignment.setCourse(new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date()));
        }
        
        @Test
        @DisplayName("Should pass validation for valid assignment")
        void testValidateAssignmentValid() {
            assertDoesNotThrow(() -> assignmentService.validateAssignment(validAssignment));
        }
        
        @Test
        @DisplayName("Should throw FailedToParseFormDataException when validation fails")
        void testValidateAssignmentInvalid() {
            validAssignment.setName(" ");
            assertThrows(FailedToParseFormDataException.class, 
                () -> assignmentService.validateAssignment(validAssignment));
        }
        
        @Test
        @DisplayName("Should include all violation messages in exception")
        void testValidateAssignmentMultipleViolations() {
            validAssignment.setName(null);
            validAssignment.setDueDate(null);
            FailedToParseFormDataException exception = assertThrows(
                FailedToParseFormDataException.class,
                () -> assignmentService.validateAssignment(validAssignment)
            );
            
            assertTrue(exception.getMessage().contains("name"));
            assertTrue(exception.getMessage().contains("dueDate"));
        }
    }

    @Nested
    @DisplayName("getAllAssignments Tests")
    class GetAllAssignmentsTests {
        @Test
        @DisplayName("Admin should receive all assignments from repository")
        void adminGetsAllAssignments() {
            List<AssignmentModel> assignments = List.of(new AssignmentModel(), new AssignmentModel());
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findAll()).thenReturn(assignments);

            List<AssignmentModel> result = assignmentService.getAllAssignments();

            assertEquals(assignments, result);
            verify(courseService, never()).getAllCourses();
        }

        @Test
        @DisplayName("Non-admin aggregates assignments from enrolled courses")
        void nonAdminGetsAssignmentsFromCourses() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel a1 = buildAssignment("a1", course);
            AssignmentModel a2 = buildAssignment("a2", course);
            course.getAssignments().addAll(List.of(a1, a2));

            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(courseService.getAllCourses()).thenReturn(List.of(course));

            List<AssignmentModel> result = assignmentService.getAllAssignments();

            assertEquals(2, result.size());
            assertTrue(result.containsAll(List.of(a1, a2)));
            verify(assignmentRepository, never()).findAll();
        }

        @Test
        @DisplayName("Non-admin returns empty list when no courses")
        void nonAdminNoCoursesReturnsEmpty() {
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(courseService.getAllCourses()).thenReturn(List.of());

            List<AssignmentModel> result = assignmentService.getAllAssignments();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAssignmentById Tests")
    class GetAssignmentByIdTests {
        @Test
        @DisplayName("Returns assignment when found")
        void returnsAssignmentWhenFound() {
            AssignmentModel assignment = buildAssignment("a1", new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date()));
            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(assignment));

            AssignmentModel result = assignmentService.getAssignmentById("a1");

            assertEquals(assignment, result);
        }

        @Test
        @DisplayName("Throws when assignment not found")
        void throwsWhenNotFound() {
            when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(AssignmentNotFoundException.class, () -> assignmentService.getAssignmentById("missing"));
        }
    }

    @Nested
    @DisplayName("createAssignment Tests")
    class CreateAssignmentTests {
        @Test
        @DisplayName("Creates assignment without files")
        void createWithoutFiles() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel assignment = buildAssignment(null, null);
            AssignmentModel saved = buildAssignment("saved-id", course);

            when(courseService.getCourseById("course-1")).thenReturn(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(assignment)).thenReturn(saved);

            AssignmentModel result = assignmentService.createAssignment(assignment, "course-1");

            assertEquals(saved, result);
            assertEquals(course, assignment.getCourse());
            verify(assignmentRepository, times(1)).save(assignment);
        }

        @Test
        @DisplayName("Rejects oversized file before saving")
        void createWithOversizedFile() throws Exception {
            ReflectionTestUtils.setField(assignmentService, "fileUploadSizeLimit", 5L);
            MockMultipartFile bigFile = new MockMultipartFile("file", "f.txt", "text/plain", new byte[10]);

            assertThrows(FileSizeLimitExceededException.class, () ->
                assignmentService.createAssignment(new AssignmentModel(), "course-1", List.of(bigFile))
            );

            verify(assignmentRepository, never()).save(any(AssignmentModel.class));
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }

        @Test
        @DisplayName("Creates assignment with files and saves twice")
        void createWithFiles() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel assignment = buildAssignment(null, null);
            AssignmentModel saved = buildAssignment("saved-id", course);
            FileModel savedFile = buildFile("file-1");
            MockMultipartFile upload = new MockMultipartFile("file", "a.txt", "text/plain", new byte[4]);

            when(courseService.getCourseById("course-1")).thenReturn(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(saved, saved);
            when(fileService.saveAssignmentFiles(List.of(upload), saved, "course-1"))
                .thenReturn(List.of(savedFile));

            AssignmentModel result = assignmentService.createAssignment(assignment, "course-1", List.of(upload));

            assertEquals(course, assignment.getCourse());
            assertEquals(1, result.getFiles().size());
            assertEquals(savedFile, result.getFiles().get(0));
            verify(assignmentRepository, times(2)).save(any(AssignmentModel.class));
            verify(fileService).saveAssignmentFiles(List.of(upload), saved, "course-1");
        }

        @Test
        @DisplayName("Create with null files saves once")
        void createWithNullFiles() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel assignment = buildAssignment(null, null);
            AssignmentModel saved = buildAssignment("saved-id", course);

            when(courseService.getCourseById("course-1")).thenReturn(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(assignment)).thenReturn(saved);

            AssignmentModel result = assignmentService.createAssignment(assignment, "course-1", null);

            assertEquals(saved, result);
            verify(assignmentRepository, times(1)).save(any(AssignmentModel.class));
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }

        @Test
        @DisplayName("Unauthorized create prevents save")
        void createUnauthorized() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel assignment = buildAssignment(null, null);
            when(courseService.getCourseById("course-1")).thenReturn(course);
            org.mockito.Mockito.doThrow(new UserNotAuthorizedException(""))
                .when(courseService).userAllowedToEditCourse(course);

            assertThrows(UserNotAuthorizedException.class,
                () -> assignmentService.createAssignment(assignment, "course-1"));

            verify(assignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateAssignment Tests")
    class UpdateAssignmentTests {
        @Test
        @DisplayName("Updates fields when authorized")
        void updatesAssignment() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            AssignmentModel updated = buildAssignment(null, course);
            updated.setName("Updated");
            updated.setDescription("New desc");
            updated.setVisible(false);
            Date newDue = new Date(System.currentTimeMillis() + 1000);
            Date updatedDate = new Date(System.currentTimeMillis() + 2000);
            updated.setDueDate(newDue);
            updated.setDateUpdated(updatedDate);

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.updateAssignment("a1", updated);

            assertEquals("Updated", result.getName());
            assertEquals("New desc", result.getDescription());
            assertEquals(false, result.getVisible());
            assertEquals(newDue, result.getDueDate());
            assertEquals(updatedDate, result.getDateUpdated());
            verify(assignmentRepository).save(existing);
        }

        @Test
        @DisplayName("Update with oversized file is blocked")
        void updateWithOversizedFile() throws Exception {
            ReflectionTestUtils.setField(assignmentService, "fileUploadSizeLimit", 1L);
            MockMultipartFile bigFile = new MockMultipartFile("file", "f.txt", "text/plain", new byte[5]);

            assertThrows(FileSizeLimitExceededException.class, () -> assignmentService.updateAssignment(
                "a1",
                new AssignmentModel(),
                List.of(bigFile),
                null,
                false
            ));

            verify(assignmentRepository, never()).findById(any());
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }

        @Test
        @DisplayName("Update with replaceAll removes old files and adds new ones")
        void updateReplaceAll() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            FileModel old1 = buildFile("f1");
            FileModel old2 = buildFile("f2");
            existing.getFiles().addAll(List.of(old1, old2));

            AssignmentModel updated = buildAssignment(null, course);
            updated.setName("Updated");
            MockMultipartFile upload = new MockMultipartFile("file", "a.txt", "text/plain", new byte[4]);
            FileModel newFile = buildFile("nf1");

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAssignmentFiles(List.of(upload), existing, "course-1"))
                .thenReturn(List.of(newFile));
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.updateAssignment("a1", updated, List.of(upload), null, true);

            assertEquals(1, result.getFiles().size());
            assertEquals(newFile, result.getFiles().get(0));
            verify(fileService, times(2)).deleteFile(any(FileModel.class));
            verify(fileService).saveAssignmentFiles(List.of(upload), existing, "course-1");
            verify(assignmentRepository).save(existing);
        }

        @Test
        @DisplayName("Update selectively removes files")
        void updateSelectiveRemove() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            FileModel keep = buildFile("keep");
            FileModel remove = buildFile("remove");
            existing.getFiles().addAll(List.of(keep, remove));

            AssignmentModel updated = buildAssignment(null, course);

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.updateAssignment(
                "a1",
                updated,
                null,
                List.of("remove"),
                false
            );

            assertEquals(1, result.getFiles().size());
            assertEquals(keep, result.getFiles().get(0));
            verify(fileService).deleteFile(remove);
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }

        @Test
        @DisplayName("Update adds files without removal")
        void updateAddFilesOnly() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            FileModel keep = buildFile("keep");
            existing.getFiles().add(keep);

            AssignmentModel updated = buildAssignment(null, course);
            MockMultipartFile upload = new MockMultipartFile("file", "a.txt", "text/plain", new byte[4]);
            FileModel newFile = buildFile("nf1");

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAssignmentFiles(List.of(upload), existing, "course-1"))
                .thenReturn(List.of(newFile));
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.updateAssignment(
                "a1",
                updated,
                List.of(upload),
                null,
                false
            );

            assertEquals(2, result.getFiles().size());
            assertTrue(result.getFiles().containsAll(List.of(keep, newFile)));
            verify(fileService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("Update adds files when list initially null")
        void updateAddsWhenFilesNull() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            existing.setFiles(null);

            AssignmentModel updated = buildAssignment(null, course);
            MockMultipartFile upload = new MockMultipartFile("file", "a.txt", "text/plain", new byte[4]);
            FileModel newFile = buildFile("nf1");

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAssignmentFiles(List.of(upload), existing, "course-1"))
                .thenReturn(List.of(newFile));
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.updateAssignment(
                "a1",
                updated,
                List.of(upload),
                null,
                false
            );

            assertEquals(1, result.getFiles().size());
            assertEquals(newFile, result.getFiles().get(0));
        }

        @Test
        @DisplayName("Update throws when unauthorized")
        void updateUnauthorized() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            org.mockito.Mockito.doThrow(new UserNotAuthorizedException(""))
                .when(courseService).userAllowedToEditCourse(course);

            assertThrows(UserNotAuthorizedException.class,
                () -> assignmentService.updateAssignment("a1", new AssignmentModel()));

            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Update throws when assignment missing")
        void updateNotFound() {
            when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(AssignmentNotFoundException.class,
                () -> assignmentService.updateAssignment("missing", new AssignmentModel()));
        }
    }

    @Nested
    @DisplayName("patchAssignment Tests")
    class PatchAssignmentTests {
        @Test
        @DisplayName("Patches only provided fields")
        void patchesPartialFields() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            existing.setDescription("Old");
            AssignmentModel patch = new AssignmentModel();
            patch.setDescription("New desc");
            Date newDue = new Date(System.currentTimeMillis() + 5000);
            patch.setDueDate(newDue);
            patch.setVisible(false);
            Date updatedDate = new Date(System.currentTimeMillis() + 6000);
            patch.setDateUpdated(updatedDate);
            CourseModel newCourse = new CourseModel("course-2", "Course2", "C2", null, null, new Date(), new Date());
            patch.setCourse(newCourse);

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment("a1", patch);

            assertEquals("New desc", result.getDescription());
            assertEquals(existing.getName(), result.getName());
            assertEquals(newDue, result.getDueDate());
            assertEquals(false, result.getVisible());
            assertEquals(updatedDate, result.getDateUpdated());
            assertEquals(newCourse, result.getCourse());
            verify(assignmentRepository).save(existing);
        }

        @Test
        @DisplayName("Patch with files adds and removes correctly")
        void patchWithFiles() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            FileModel keep = buildFile("keep");
            FileModel remove = buildFile("remove");
            existing.getFiles().addAll(List.of(keep, remove));

            AssignmentModel patch = new AssignmentModel();
            patch.setDescription("Patched");
            MockMultipartFile upload = new MockMultipartFile("file", "a.txt", "text/plain", new byte[4]);
            FileModel newFile = buildFile("nf1");

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAssignmentFiles(List.of(upload), existing, "course-1"))
                .thenReturn(List.of(newFile));
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment(
                "a1",
                patch,
                List.of(upload),
                List.of("remove"),
                false
            );

            assertEquals("Patched", result.getDescription());
            assertEquals(2, result.getFiles().size());
            assertTrue(result.getFiles().containsAll(List.of(keep, newFile)));
            verify(fileService).deleteFile(remove);
            verify(fileService).saveAssignmentFiles(List.of(upload), existing, "course-1");
        }

        @Test
        @DisplayName("Patch with replaceAll clears and adds new files")
        void patchReplaceAll() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            FileModel old = buildFile("old");
            existing.getFiles().add(old);

            AssignmentModel patch = new AssignmentModel();
            MockMultipartFile upload = new MockMultipartFile("file", "a.txt", "text/plain", new byte[4]);
            FileModel newFile = buildFile("nf1");

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAssignmentFiles(List.of(upload), existing, "course-1"))
                .thenReturn(List.of(newFile));
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment(
                "a1",
                patch,
                List.of(upload),
                null,
                true
            );

            assertEquals(1, result.getFiles().size());
            assertEquals(newFile, result.getFiles().get(0));
            verify(fileService).deleteFile(old);
        }

        @Test
        @DisplayName("Patch with oversized file is blocked early")
        void patchOversizedFile() {
            ReflectionTestUtils.setField(assignmentService, "fileUploadSizeLimit", 1L);
            MockMultipartFile bigFile = new MockMultipartFile("file", "f.txt", "text/plain", new byte[5]);

            assertThrows(FileSizeLimitExceededException.class, () -> assignmentService.patchAssignment(
                "a1",
                new AssignmentModel(),
                List.of(bigFile),
                null,
                false
            ));

            verify(assignmentRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Patch throws when assignment missing")
        void patchNotFound() {
            when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(AssignmentNotFoundException.class,
                () -> assignmentService.patchAssignment("missing", new AssignmentModel()));
        }

        @Test
        @DisplayName("Patch throws when unauthorized")
        void patchUnauthorized() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            org.mockito.Mockito.doThrow(new UserNotAuthorizedException(""))
                .when(courseService).userAllowedToEditCourse(course);

            assertThrows(UserNotAuthorizedException.class,
                () -> assignmentService.patchAssignment("a1", new AssignmentModel()));

            verify(assignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteAssignment Tests")
    class DeleteAssignmentTests {
        @Test
        @DisplayName("Deletes assignment when authorized")
        void deletesAuthorized() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel assignment = buildAssignment("a1", course);
            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(assignment));
            doNothing().when(courseService).userAllowedToEditCourse(course);

            assignmentService.deleteAssignment("a1");

            verify(assignmentRepository).deleteById("a1");
        }

        @Test
        @DisplayName("Delete throws when unauthorized")
        void deleteUnauthorized() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel assignment = buildAssignment("a1", course);
            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(assignment));
            org.mockito.Mockito.doThrow(new UserNotAuthorizedException(""))
                .when(courseService).userAllowedToEditCourse(course);

            assertThrows(UserNotAuthorizedException.class, () -> assignmentService.deleteAssignment("a1"));

            verify(assignmentRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Delete throws when assignment missing")
        void deleteNotFound() {
            when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(AssignmentNotFoundException.class, () -> assignmentService.deleteAssignment("missing"));
        }
    }

    // Helpers
    private AssignmentModel buildAssignment(String id, CourseModel course) {
        AssignmentModel assignment = new AssignmentModel();
        assignment.setAssignmentId(id);
        assignment.setName("Assignment");
        assignment.setDescription("Desc");
        assignment.setVisible(true);
        assignment.setDueDate(new Date());
        if (course != null) {
            assignment.setCourse(course);
        }
        return assignment;
    }

    private FileModel buildFile(String id) {
        FileModel file = new FileModel("name", "path", "type", "content/type");
        ReflectionTestUtils.setField(file, "fileId", id);
        return file;
    }

}


