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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
        void testValidateAssignment_Valid() {
            assertDoesNotThrow(() -> assignmentService.validateAssignment(validAssignment));
        }
        
        @Test
        @DisplayName("Should throw FailedToParseFormDataException when validation fails")
        void testValidateAssignment_Invalid() {
            validAssignment.setName(" ");
            assertThrows(FailedToParseFormDataException.class, 
                () -> assignmentService.validateAssignment(validAssignment));
        }
        
        @Test
        @DisplayName("Should include all violation messages in exception")
        void testValidateAssignment_MultipleViolations() {
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
        @DisplayName("Admin should get all assignments")
        void testGetAllAssignments_AdminGetsAllAssignments() {
            List<AssignmentModel> assignments = List.of(new AssignmentModel(), new AssignmentModel());
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findAll()).thenReturn(assignments);

            List<AssignmentModel> result = assignmentService.getAllAssignments();

            assertEquals(assignments, result);
            verify(courseService, never()).getAllCourses();
        }

        @ParameterizedTest
        @MethodSource("provideNonAdminUsers")
        @DisplayName("{0} aggregates assignments from enrolled courses")
        void testGetAllAssignments_nonAdminGetsAssignmentsFromCourses(Role role, UserModel user) {
            CourseModel courseOne = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            CourseModel courseTwo = new CourseModel("course-2", "Course 2", "C2", null, null, new Date(), new Date());
            AssignmentModel a1 = buildAssignment("a1", courseOne);
            AssignmentModel a2 = buildAssignment("a2", courseOne);
            AssignmentModel b1 = buildAssignment("b1", courseTwo);
            courseOne.addAssignment(a1);
            courseOne.addAssignment(a2);
            courseTwo.addAssignment(b1);

            when(authService.getCurrentUser()).thenReturn(user);
            when(courseService.getAllCourses()).thenReturn(List.of(courseOne, courseTwo));

            List<AssignmentModel> result = assignmentService.getAllAssignments();

            assertEquals(3, result.size());
            assertTrue(result.containsAll(List.of(a1, a2, b1)));
            verify(assignmentRepository, never()).findAll();
        }

        @Test
        @DisplayName("Non-admin returns empty list when no courses")
        void testGetAllAssignments_nonAdminNoCoursesReturnsEmpty() {
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(courseService.getAllCourses()).thenReturn(List.of());

            List<AssignmentModel> result = assignmentService.getAllAssignments();

            assertTrue(result.isEmpty());
        }

        static Stream<Arguments> provideNonAdminUsers() {
            return Stream.of(
                Arguments.of(Role.INSTRUCTOR, instructorUser),
                Arguments.of(Role.STUDENT, studentUser)
            );
        }
    }

    @Nested
    @DisplayName("getAssignmentById Tests")
    class GetAssignmentByIdTests {
        @Test
        @DisplayName("Returns assignment when found")
        void testGetAssignmentsById_returnsAssignmentWhenFound() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel assignment = buildAssignment("a1", course);
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(assignment));

            AssignmentModel result = assignmentService.getAssignmentById("a1");

            assertEquals(assignment, result);
            verify(courseService).userAllowedToAccessCourse(course);
        }

        @Test
        @DisplayName("Throws when assignment not found")
        void testGetAssignmentsById_throwsWhenNotFound() {
            when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(AssignmentNotFoundException.class, () -> assignmentService.getAssignmentById("missing"));
            verify(courseService, never()).userAllowedToAccessCourse(any());
        }

        @Test
        @DisplayName("Throws when user not allowed to access assignment")
        void testGetAssignmentsById_throwsWhenUnauthorizedAccess() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel assignment = buildAssignment("a1", course);
            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(assignment));
            org.mockito.Mockito.doThrow(new UserNotAuthorizedException(""))
                .when(courseService).userAllowedToAccessCourse(course);

            assertThrows(UserNotAuthorizedException.class, () -> assignmentService.getAssignmentById("a1"));

            verify(courseService).userAllowedToAccessCourse(course);
        }
    }

    @Nested
    @DisplayName("createAssignment Tests")
    class CreateAssignmentTests {
        @Test
        @DisplayName("Creates assignment without files")
        void testCreateAssignment_createWithoutFiles() {
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
        @DisplayName("Rejects oversized files")
        void testCreateAssignment_createWithOversizedFile() throws Exception {
            ReflectionTestUtils.setField(assignmentService, "fileUploadSizeLimit", 5L);
            MockMultipartFile bigFile = new MockMultipartFile("file", "f.txt", "text/plain", new byte[10]);

            assertThrows(FileSizeLimitExceededException.class, () ->
                assignmentService.createAssignment(new AssignmentModel(), "course-1", List.of(bigFile))
            );

            verify(assignmentRepository, never()).save(any(AssignmentModel.class));
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }

        @Test
        @DisplayName("Creates assignment with files")
        void testCreateAssignment_createWithFiles() throws Exception {
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
        @DisplayName("Create with null files")
        void testCreateAssignment_createWithNullFiles() throws Exception {
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
        @DisplayName("Unauthorized create throws UserNotAuthorizedException")
        void testCreateAssignment_createUnauthorized() {
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
        void testUpdateAssignment_updatesAssignment() {
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
        void testUpdateAssignment_updateWithOversizedFile() throws IOException {
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
        void testUpdateAssignment_replaceAll() throws Exception {
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
        void testUpdateAssignment_updateSelectiveRemove() throws Exception {
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
        void testUpdateAssignment_updateAddFilesOnly() throws Exception {
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
        void testUpdateAssignment_updateAddsWhenFilesNull() throws Exception {
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
        void testUpdateAssignment_updateUnauthorized() {
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
        void testUpdateAssignment_updateNotFound() {
            when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(AssignmentNotFoundException.class,
                () -> assignmentService.updateAssignment("missing", new AssignmentModel()));
        }
    }

    @Nested
    @DisplayName("patchAssignment Tests")
    class PatchAssignmentTests {
        @Test
        @DisplayName("Patches name field only")
        void testPatchAssignment_patchNameOnly() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            String originalDesc = existing.getDescription();
            Boolean originalVisible = existing.getVisible();
            Date originalDue = existing.getDueDate();

            AssignmentModel patch = new AssignmentModel();
            patch.setName("New Name");

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment("a1", patch);

            assertEquals("New Name", result.getName());
            assertEquals(originalDesc, result.getDescription());
            assertEquals(originalVisible, result.getVisible());
            assertEquals(originalDue, result.getDueDate());
            verify(assignmentRepository).save(existing);
        }

        @Test
        @DisplayName("Patches description field only")
        void testPatchAssignment_patchDescriptionOnly() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            String originalName = existing.getName();
            Boolean originalVisible = existing.getVisible();
            Date originalDue = existing.getDueDate();

            AssignmentModel patch = new AssignmentModel();
            patch.setDescription("New description");

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment("a1", patch);

            assertEquals("New description", result.getDescription());
            assertEquals(originalName, result.getName());
            assertEquals(originalVisible, result.getVisible());
            assertEquals(originalDue, result.getDueDate());
            verify(assignmentRepository).save(existing);
        }

        @Test
        @DisplayName("Patches visible field only")
        void testPatchAssignment_patchVisibleOnly() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            existing.setVisible(true);
            String originalName = existing.getName();
            String originalDesc = existing.getDescription();
            Date originalDue = existing.getDueDate();

            AssignmentModel patch = new AssignmentModel();
            patch.setVisible(false);

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment("a1", patch);

            assertEquals(false, result.getVisible());
            assertEquals(originalName, result.getName());
            assertEquals(originalDesc, result.getDescription());
            assertEquals(originalDue, result.getDueDate());
            verify(assignmentRepository).save(existing);
        }

        @Test
        @DisplayName("Patches dueDate field only")
        void testPatchAssignment_patchDueDateOnly() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            String originalName = existing.getName();
            String originalDesc = existing.getDescription();
            Boolean originalVisible = existing.getVisible();
            Date newDue = new Date(System.currentTimeMillis() + 5000);

            AssignmentModel patch = new AssignmentModel();
            patch.setDueDate(newDue);

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment("a1", patch);

            assertEquals(newDue, result.getDueDate());
            assertEquals(originalName, result.getName());
            assertEquals(originalDesc, result.getDescription());
            assertEquals(originalVisible, result.getVisible());
            verify(assignmentRepository).save(existing);
        }

        @Test
        @DisplayName("Patches dateUpdated field only")
        void testPatchAssignment_patchDateUpdatedOnly() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            String originalName = existing.getName();
            String originalDesc = existing.getDescription();
            Boolean originalVisible = existing.getVisible();
            Date originalDue = existing.getDueDate();
            Date updatedDate = new Date(System.currentTimeMillis() + 6000);

            AssignmentModel patch = new AssignmentModel();
            patch.setDateUpdated(updatedDate);

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment("a1", patch);

            assertEquals(updatedDate, result.getDateUpdated());
            assertEquals(originalName, result.getName());
            assertEquals(originalDesc, result.getDescription());
            assertEquals(originalVisible, result.getVisible());
            assertEquals(originalDue, result.getDueDate());
            verify(assignmentRepository).save(existing);
        }

        @Test
        @DisplayName("Patches course field only")
        void testPatchAssignment_patchCourseOnly() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            String originalName = existing.getName();
            String originalDesc = existing.getDescription();
            Boolean originalVisible = existing.getVisible();
            Date originalDue = existing.getDueDate();

            CourseModel newCourse = new CourseModel("course-2", "Course2", "C2", null, null, new Date(), new Date());
            AssignmentModel patch = new AssignmentModel();
            patch.setCourse(newCourse);

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment("a1", patch);

            assertEquals(newCourse, result.getCourse());
            assertEquals(originalName, result.getName());
            assertEquals(originalDesc, result.getDescription());
            assertEquals(originalVisible, result.getVisible());
            assertEquals(originalDue, result.getDueDate());
            verify(assignmentRepository).save(existing);
        }

        @Test
        @DisplayName("Patch adds files only without field updates")
        void testPatchAssignment_patchAddFilesOnly() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            String originalName = existing.getName();
            String originalDesc = existing.getDescription();

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
                false
            );

            assertEquals(1, result.getFiles().size());
            assertEquals(newFile, result.getFiles().get(0));
            assertEquals(originalName, result.getName());
            assertEquals(originalDesc, result.getDescription());
            verify(fileService).saveAssignmentFiles(List.of(upload), existing, "course-1");
            verify(fileService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("Patch removes files only without field updates")
        void testPatchAssignment_patchRemoveFilesOnly() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            FileModel keep = buildFile("keep");
            FileModel remove = buildFile("remove");
            existing.getFiles().addAll(List.of(keep, remove));
            String originalName = existing.getName();
            String originalDesc = existing.getDescription();

            AssignmentModel patch = new AssignmentModel();

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment(
                "a1",
                patch,
                null,
                List.of("remove"),
                false
            );

            assertEquals(1, result.getFiles().size());
            assertEquals(keep, result.getFiles().get(0));
            assertEquals(originalName, result.getName());
            assertEquals(originalDesc, result.getDescription());
            verify(fileService).deleteFile(remove);
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }

        @Test
        @DisplayName("Patch adds and removes files in same operation")
        void testPatchAssignment_patchAddAndRemoveFiles() throws Exception {
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
        @DisplayName("Patch with replaceAll removes all existing and adds new files")
        void testPatchAssignment_patchReplaceAll() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            FileModel old1 = buildFile("old1");
            FileModel old2 = buildFile("old2");
            existing.getFiles().addAll(List.of(old1, old2));

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
            verify(fileService).deleteFile(old1);
            verify(fileService).deleteFile(old2);
            verify(fileService).saveAssignmentFiles(List.of(upload), existing, "course-1");
        }

        @Test
        @DisplayName("Patch adds files when files collection is initially null")
        void testPatchAssignment_patchAddFilesWhenNull() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            existing.setFiles(null);

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
                false
            );

            assertEquals(1, result.getFiles().size());
            assertEquals(newFile, result.getFiles().get(0));
            verify(fileService).saveAssignmentFiles(List.of(upload), existing, "course-1");
        }

        @Test
        @DisplayName("Patch with no file operations keeps existing files")
        void testPatchAssignment_patchNoFileOperations() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            FileModel existing1 = buildFile("existing1");
            existing.getFiles().add(existing1);

            AssignmentModel patch = new AssignmentModel();
            patch.setDescription("Patched");

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment(
                "a1",
                patch,
                null,
                null,
                false
            );

            assertEquals("Patched", result.getDescription());
            assertEquals(1, result.getFiles().size());
            assertEquals(existing1, result.getFiles().get(0));
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
            verify(fileService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("Patch with empty files list keeps existing files")
        void testPatchAssignment_patchWithEmptyFilesList() throws Exception {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel existing = buildAssignment("a1", course);
            FileModel existing1 = buildFile("existing1");
            existing.getFiles().add(existing1);

            AssignmentModel patch = new AssignmentModel();
            patch.setName("New Name");

            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(assignmentRepository.save(existing)).thenReturn(existing);

            AssignmentModel result = assignmentService.patchAssignment(
                "a1",
                patch,
                List.of(),
                null,
                false
            );

            assertEquals("New Name", result.getName());
            assertEquals(1, result.getFiles().size());
            assertEquals(existing1, result.getFiles().get(0));
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }

        @Test
        @DisplayName("Patch with oversized file is blocked before database access")
        void testPatchAssignment_patchOversizedFile() {
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
            verify(courseService, never()).userAllowedToEditCourse(any(CourseModel.class));
        }

        @Test
        @DisplayName("Patch throws when assignment missing")
        void testPatchAssignment_patchNotFound() {
            when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(AssignmentNotFoundException.class,
                () -> assignmentService.patchAssignment("missing", new AssignmentModel()));
        }

        @Test
        @DisplayName("Patch throws when unauthorized")
        void testPatchAssignment_patchUnauthorized() {
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
        void testDeleteAssignment_deletesAuthorized() {
            CourseModel course = new CourseModel("course-1", "Course", "C1", null, null, new Date(), new Date());
            AssignmentModel assignment = buildAssignment("a1", course);
            when(assignmentRepository.findById("a1")).thenReturn(Optional.of(assignment));
            doNothing().when(courseService).userAllowedToEditCourse(course);

            assignmentService.deleteAssignment("a1");

            verify(assignmentRepository).deleteById("a1");
        }

        @Test
        @DisplayName("Delete throws when unauthorized")
        void testDeleteAssignment_deleteUnauthorized() {
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
        void testDeleteAssignment_deleteNotFound() {
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


