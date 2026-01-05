package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import peerport.backend.database.AssignmentsRepository;
import peerport.backend.exceptions.assignments.AssignmentNotFoundException;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.*;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.service.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssignmentService Unit Tests")
public class AssignmentServiceTest {
    
    @InjectMocks
    private AssignmentService assignmentService;
    
    @Mock
    private AssignmentsRepository assignmentRepository;
    
    @Mock
    private AuthService authService;
    
    @Mock
    private CourseService courseService;
    
    @Mock
    private FileService fileService;

    private Validator validator;

    private static UserModel adminUser, instructorUser, studentUser;
    private static final long FILE_SIZE_LIMIT = 5242880L; // 5MB

    @BeforeAll
    static void initAll() {
        adminUser = new UserModel("admin", "admin", "admin", null, null, Role.ADMIN);
        instructorUser = new UserModel("instructor", "instructor", "instructor", null, null, Role.INSTRUCTOR);
        studentUser = new UserModel("student", "student", "student", null, null, Role.STUDENT);
    }

    @BeforeEach
    void setUp() {
        initAll();
        
        // Initialize validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        ReflectionTestUtils.setField(assignmentService, "validator", validator);
        ReflectionTestUtils.setField(assignmentService, "fileUploadSizeLimit", FILE_SIZE_LIMIT);
    }

    @Nested
    @DisplayName("validateAssignment Tests")
    class ValidateAssignmentTests {
        
        @Test
        @DisplayName("Valid assignment does not throw exception")
        void testValidateAssignment_Valid_NoException() {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            
            // Act & Assert
            assertDoesNotThrow(() -> assignmentService.validateAssignment(assignment));
        }
        
        @Test
        @DisplayName("Invalid assignment with null name throws exception")
        void testValidateAssignment_NullName_ThrowsException() {
            // Arrange
            AssignmentModel assignment = new AssignmentModel();
            assignment.setDueDate(new Date());
            // name is null
            
            // Act & Assert
            assertThrows(FailedToParseFormDataException.class, () -> 
                assignmentService.validateAssignment(assignment));
        }
        
        @Test
        @DisplayName("Invalid assignment with blank name throws exception")
        void testValidateAssignment_BlankName_ThrowsException() {
            // Arrange
            AssignmentModel assignment = new AssignmentModel();
            assignment.setName("");
            assignment.setDueDate(new Date());
            
            // Act & Assert
            assertThrows(FailedToParseFormDataException.class, () -> 
                assignmentService.validateAssignment(assignment));
        }
        
        @Test
        @DisplayName("Invalid assignment with null dueDate throws exception")
        void testValidateAssignment_NullDueDate_ThrowsException() {
            // Arrange
            AssignmentModel assignment = new AssignmentModel();
            assignment.setName("Valid Name");
            // dueDate is null
            
            // Act & Assert
            assertThrows(FailedToParseFormDataException.class, () -> 
                assignmentService.validateAssignment(assignment));
        }
        
        @Test
        @DisplayName("Invalid assignment with description too long throws exception")
        void testValidateAssignment_DescriptionTooLong_ThrowsException() {
            // Arrange
            AssignmentModel assignment = new AssignmentModel();
            assignment.setName("Valid Name");
            assignment.setDueDate(new Date());
            assignment.setDescription("a".repeat(2001)); // Exceeds 2000 character limit
            
            // Act & Assert
            assertThrows(FailedToParseFormDataException.class, () -> 
                assignmentService.validateAssignment(assignment));
        }
    }

    @Nested
    @DisplayName("createAssignment Tests")
    class CreateAssignmentTests {
        
        @Test
        @DisplayName("Admin creates assignment without files successfully")
        void testCreateAssignment_AdminNoFiles_Success() throws IOException {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            AssignmentModel savedAssignment = createValidAssignment();
            savedAssignment.setAssignmentId("assignment-1");
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(courseService.getCourseById("course-1")).thenReturn(course);
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(savedAssignment);
            
            // Act
            AssignmentModel result = assignmentService.createAssignment(assignment, "course-1");
            
            // Assert
            assertNotNull(result);
            assertEquals("assignment-1", result.getAssignmentId());
            verify(assignmentRepository, times(1)).save(any(AssignmentModel.class));
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }
        
        @Test
        @DisplayName("Instructor creates assignment without files successfully")
        void testCreateAssignment_InstructorNoFiles_Success() {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            CourseModel course = createCourse("course-1");
            course.addInstructor(instructorUser);
            AssignmentModel savedAssignment = createValidAssignment();
            savedAssignment.setAssignmentId("assignment-1");
            
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(courseService.getCourseById("course-1")).thenReturn(course);
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(savedAssignment);
            
            // Act
            AssignmentModel result = assignmentService.createAssignment(assignment, "course-1");
            
            // Assert
            assertNotNull(result);
            assertEquals("assignment-1", result.getAssignmentId());
            verify(assignmentRepository, times(1)).save(any(AssignmentModel.class));
        }
        
        @Test
        @DisplayName("Create assignment with files successfully")
        void testCreateAssignment_WithFiles_Success() throws IOException {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            AssignmentModel savedAssignment = createValidAssignment();
            savedAssignment.setAssignmentId("assignment-1");
            savedAssignment.setFiles(new ArrayList<>());
            
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
            List<MultipartFile> files = Arrays.asList(file);
            List<FileModel> savedFiles = Arrays.asList(new FileModel("test.pdf", "/path", "pdf", "application/pdf"));
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(courseService.getCourseById("course-1")).thenReturn(course);
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(savedAssignment);
            when(fileService.saveAssignmentFiles(anyList(), any(AssignmentModel.class), eq("course-1")))
                .thenReturn(savedFiles);
            
            // Act
            AssignmentModel result = assignmentService.createAssignment(assignment, "course-1", files);
            
            // Assert
            assertNotNull(result);
            assertEquals("assignment-1", result.getAssignmentId());
            verify(assignmentRepository, times(2)).save(any(AssignmentModel.class));
            verify(fileService, times(1)).saveAssignmentFiles(anyList(), any(AssignmentModel.class), eq("course-1"));
        }
        
        @Test
        @DisplayName("Create assignment with file too large throws exception")
        void testCreateAssignment_FileTooLarge_ThrowsException() {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            MockMultipartFile largeFile = new MockMultipartFile("file", "big.pdf", "application/pdf", 
                new byte[10 * 1024 * 1024]); // 10MB
            List<MultipartFile> files = Arrays.asList(largeFile);
            
            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> 
                assignmentService.createAssignment(assignment, "course-1", files));
            verify(assignmentRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Create assignment with empty files list succeeds")
        void testCreateAssignment_EmptyFilesList_Success() throws IOException {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            AssignmentModel savedAssignment = createValidAssignment();
            savedAssignment.setAssignmentId("assignment-1");
            savedAssignment.setFiles(new ArrayList<>());
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(courseService.getCourseById("course-1")).thenReturn(course);
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(savedAssignment);
            
            // Act
            AssignmentModel result = assignmentService.createAssignment(assignment, "course-1", new ArrayList<>());
            
            // Assert
            assertNotNull(result);
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }
        
        @Test
        @DisplayName("Create assignment with null files list succeeds")
        void testCreateAssignment_NullFilesList_Success() throws IOException {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            AssignmentModel savedAssignment = createValidAssignment();
            savedAssignment.setAssignmentId("assignment-1");
            savedAssignment.setFiles(new ArrayList<>());
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(courseService.getCourseById("course-1")).thenReturn(course);
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(savedAssignment);
            
            // Act
            AssignmentModel result = assignmentService.createAssignment(assignment, "course-1", null);
            
            // Assert
            assertNotNull(result);
            verify(fileService, never()).saveAssignmentFiles(any(), any(), any());
        }
        
        @Test
        @DisplayName("Unauthorized user cannot create assignment")
        void testCreateAssignment_Unauthorized_ThrowsException() {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            CourseModel course = createCourse("course-1");
            
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(courseService.getCourseById("course-1")).thenReturn(course);
            doThrow(new UserNotAuthorizedException("Not authorized"))
                .when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            
            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> 
                assignmentService.createAssignment(assignment, "course-1"));
            verify(assignmentRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("IOException propagates when saving files")
        void testCreateAssignment_IOExceptionOnSaveFiles_Propagates() throws IOException {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            AssignmentModel savedAssignment = createValidAssignment();
            savedAssignment.setAssignmentId("assignment-1");
            savedAssignment.setFiles(new ArrayList<>());
            
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
            List<MultipartFile> files = Arrays.asList(file);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(courseService.getCourseById("course-1")).thenReturn(course);
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(savedAssignment);
            when(fileService.saveAssignmentFiles(anyList(), any(AssignmentModel.class), anyString()))
                .thenThrow(new IOException("File save failed"));
            
            // Act & Assert
            assertThrows(IOException.class, () -> 
                assignmentService.createAssignment(assignment, "course-1", files));
        }
    }

    @Nested
    @DisplayName("getAllAssignments Tests")
    class GetAllAssignmentsTests {
        
        @ParameterizedTest
        @MethodSource("provideRoleUsers")
        @DisplayName("{0} user retrieves assignments successfully")
        void testGetAllAssignments_ByRoleUsers(Role role, UserModel user) {
            // Arrange
            List<AssignmentModel> assignments = Arrays.asList(
                createValidAssignment(), 
                createValidAssignment()
            );
            List<CourseModel> courses = Arrays.asList(createCourse("course-1"));
            
            setupAssignmentsForUser(user, assignments, courses, role);
            when(authService.getCurrentUser()).thenReturn(user);
            
            // Act
            List<AssignmentModel> result = assignmentService.getAllAssignments();
            
            // Assert
            assertNotNull(result);
            if (role == Role.ADMIN) {
                assertEquals(assignments.size(), result.size());
            } else {
                assertTrue(result.size() >= 0);
            }
        }
        
        @Test
        @DisplayName("Admin gets all assignments")
        void testGetAllAssignments_Admin_ReturnsAll() {
            // Arrange
            List<AssignmentModel> assignments = Arrays.asList(
                createValidAssignment(),
                createValidAssignment()
            );
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findAll()).thenReturn(assignments);
            
            // Act
            List<AssignmentModel> result = assignmentService.getAllAssignments();
            
            // Assert
            assertEquals(2, result.size());
            verify(assignmentRepository, times(1)).findAll();
        }
        
        @Test
        @DisplayName("Student gets only enrolled course assignments")
        void testGetAllAssignments_Student_ReturnsEnrolledOnly() {
            // Arrange
            CourseModel course = createCourse("course-1");
            AssignmentModel assignment1 = createValidAssignment();
            AssignmentModel assignment2 = createValidAssignment();
            course.addAssignment(assignment1);
            course.addAssignment(assignment2);
            
            studentUser.addEnrollment(new EnrollmentModel("", null, studentUser, course));
            
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(courseService.getAllCourses()).thenReturn(Arrays.asList(course));
            
            // Act
            List<AssignmentModel> result = assignmentService.getAllAssignments();
            
            // Assert
            assertEquals(2, result.size());
        }
        
        @Test
        @DisplayName("User with no courses returns empty list")
        void testGetAllAssignments_NoCourses_ReturnsEmpty() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(courseService.getAllCourses()).thenReturn(new ArrayList<>());
            
            // Act
            List<AssignmentModel> result = assignmentService.getAllAssignments();
            
            // Assert
            assertTrue(result.isEmpty());
        }
        
        static Stream<Arguments> provideRoleUsers() {
            return Stream.of(
                Arguments.of(Role.ADMIN, adminUser),
                Arguments.of(Role.INSTRUCTOR, instructorUser),
                Arguments.of(Role.STUDENT, studentUser)
            );
        }
    }

    @Nested
    @DisplayName("getAssignmentById Tests")
    class GetAssignmentByIdTests {
        
        @Test
        @DisplayName("Get assignment by id successfully")
        void testGetAssignmentById_Exists_ReturnsAssignment() {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            assignment.setAssignmentId("assignment-1");
            
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(assignment));
            
            // Act
            AssignmentModel result = assignmentService.getAssignmentById("assignment-1");
            
            // Assert
            assertNotNull(result);
            assertEquals("assignment-1", result.getAssignmentId());
        }
        
        @Test
        @DisplayName("Get assignment by non-existent id throws exception")
        void testGetAssignmentById_NotFound_ThrowsException() {
            // Arrange
            when(assignmentRepository.findById("non-existent")).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThrows(AssignmentNotFoundException.class, () -> 
                assignmentService.getAssignmentById("non-existent"));
        }
    }

    @Nested
    @DisplayName("updateAssignment Tests")
    class UpdateAssignmentTests {
        
        @Test
        @DisplayName("Admin updates assignment without files successfully")
        void testUpdateAssignment_AdminNoFiles_Success() {
            // Arrange
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setName("Old Name");
            existing.setCourse(createCourse("course-1"));
            existing.getCourse().addInstructor(adminUser);
            
            AssignmentModel updated = createValidAssignment();
            updated.setName("New Name");
            updated.setDescription("New Description");
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(existing);
            
            // Act
            AssignmentModel result = assignmentService.updateAssignment("assignment-1", updated);
            
            // Assert
            assertNotNull(result);
            assertEquals("New Name", result.getName());
            assertEquals("New Description", result.getDescription());
            verify(assignmentRepository, times(1)).save(any(AssignmentModel.class));
        }
        
        @Test
        @DisplayName("Instructor updates own course assignment")
        void testUpdateAssignment_InstructorOwnCourse_Success() {
            // Arrange
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            CourseModel course = createCourse("course-1");
            course.addInstructor(instructorUser);
            existing.setCourse(course);
            
            AssignmentModel updated = createValidAssignment();
            updated.setName("Updated Name");
            
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(existing);
            
            // Act
            AssignmentModel result = assignmentService.updateAssignment("assignment-1", updated);
            
            // Assert
            assertNotNull(result);
            assertEquals("Updated Name", result.getName());
        }
        
        @Test
        @DisplayName("Update assignment with files successfully")
        void testUpdateAssignment_WithFiles_Success() throws IOException {
            // Arrange
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setFiles(new ArrayList<>());
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            existing.setCourse(course);
            
            AssignmentModel updated = createValidAssignment();
            updated.setName("Updated Name");
            
            MockMultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", new byte[1024]);
            List<MultipartFile> files = Arrays.asList(file);
            List<FileModel> savedFiles = Arrays.asList(new FileModel("new.pdf", "/path", "pdf", "application/pdf"));
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(existing);
            when(fileService.saveAssignmentFiles(anyList(), any(AssignmentModel.class), anyString()))
                .thenReturn(savedFiles);
            
            // Act
            AssignmentModel result = assignmentService.updateAssignment("assignment-1", updated, files, null, false);
            
            // Assert
            assertNotNull(result);
            assertEquals("Updated Name", result.getName());
            verify(fileService, times(1)).saveAssignmentFiles(anyList(), any(AssignmentModel.class), anyString());
        }
        
        @Test
        @DisplayName("Update assignment with file too large throws exception")
        void testUpdateAssignment_FileTooLarge_ThrowsException() {
            // Arrange
            MockMultipartFile largeFile = new MockMultipartFile("file", "big.pdf", "application/pdf", 
                new byte[10 * 1024 * 1024]); // 10MB
            List<MultipartFile> files = Arrays.asList(largeFile);
            
            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> 
                assignmentService.updateAssignment("assignment-1", new AssignmentModel(), files, null, false));
            verify(assignmentRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Update assignment replaceAll deletes old files")
        void testUpdateAssignment_ReplaceAll_DeletesOldFiles() throws IOException {
            // Arrange
            FileModel oldFile1 = new FileModel("old1.pdf", "/path1", "pdf", "application/pdf");
            FileModel oldFile2 = new FileModel("old2.pdf", "/path2", "pdf", "application/pdf");
            
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setFiles(new ArrayList<>(Arrays.asList(oldFile1, oldFile2)));
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            existing.setCourse(course);
            
            AssignmentModel updated = createValidAssignment();
            MockMultipartFile newFile = new MockMultipartFile("file", "new.pdf", "application/pdf", new byte[1024]);
            List<MultipartFile> files = Arrays.asList(newFile);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(existing);
            when(fileService.saveAssignmentFiles(anyList(), any(AssignmentModel.class), anyString()))
                .thenReturn(new ArrayList<>());
            
            // Act
            assignmentService.updateAssignment("assignment-1", updated, files, null, true);
            
            // Assert
            verify(fileService, times(1)).deleteFile(oldFile1);
            verify(fileService, times(1)).deleteFile(oldFile2);
        }
        
        @Test
        @DisplayName("Update assignment with selective file removal")
        void testUpdateAssignment_SelectiveRemoval_Success() throws IOException {
            // Arrange
            FileModel file1 = new FileModel("file1.pdf", "/path1", "pdf", "application/pdf");
            ReflectionTestUtils.setField(file1, "fileId", "file-1");
            FileModel file2 = new FileModel("file2.pdf", "/path2", "pdf", "application/pdf");
            ReflectionTestUtils.setField(file2, "fileId", "file-2");
            
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setFiles(new ArrayList<>(Arrays.asList(file1, file2)));
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            existing.setCourse(course);
            
            AssignmentModel updated = createValidAssignment();
            List<String> removeFileIds = Arrays.asList("file-1");
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(existing);
            
            // Act
            assignmentService.updateAssignment("assignment-1", updated, null, removeFileIds, false);
            
            // Assert
            verify(fileService, times(1)).deleteFile(file1);
            verify(fileService, never()).deleteFile(file2);
        }
        
        @Test
        @DisplayName("Unauthorized user cannot update assignment")
        void testUpdateAssignment_Unauthorized_ThrowsException() {
            // Arrange
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setCourse(createCourse("course-1"));
            
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doThrow(new UserNotAuthorizedException("Not authorized"))
                .when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            
            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> 
                assignmentService.updateAssignment("assignment-1", new AssignmentModel()));
            verify(assignmentRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Update non-existent assignment throws exception")
        void testUpdateAssignment_NotFound_ThrowsException() {
            // Arrange
            when(assignmentRepository.findById("non-existent")).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThrows(AssignmentNotFoundException.class, () -> 
                assignmentService.updateAssignment("non-existent", new AssignmentModel()));
        }
    }

    @Nested
    @DisplayName("patchAssignment Tests")
    class PatchAssignmentTests {
        
        @Test
        @DisplayName("Patch assignment with only name")
        void testPatchAssignment_OnlyName_Success() {
            // Arrange
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setName("Old Name");
            existing.setDescription("Old Description");
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            existing.setCourse(course);
            
            AssignmentModel patch = new AssignmentModel();
            patch.setName("New Name");
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(existing);
            
            // Act
            AssignmentModel result = assignmentService.patchAssignment("assignment-1", patch);
            
            // Assert
            assertNotNull(result);
            assertEquals("New Name", result.getName());
            assertEquals("Old Description", result.getDescription());
        }
        
        @Test
        @DisplayName("Patch assignment with multiple fields")
        void testPatchAssignment_MultipleFields_Success() {
            // Arrange
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setName("Old Name");
            existing.setDescription("Old Description");
            existing.setVisible(false);
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            existing.setCourse(course);
            
            AssignmentModel patch = new AssignmentModel();
            patch.setName("New Name");
            patch.setVisible(true);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(existing);
            
            // Act
            AssignmentModel result = assignmentService.patchAssignment("assignment-1", patch);
            
            // Assert
            assertNotNull(result);
            assertEquals("New Name", result.getName());
            assertEquals("Old Description", result.getDescription());
            assertTrue(result.getVisible());
        }
        
        @Test
        @DisplayName("Patch assignment with files")
        void testPatchAssignment_WithFiles_Success() throws IOException {
            // Arrange
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setFiles(new ArrayList<>());
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            existing.setCourse(course);
            
            AssignmentModel patch = new AssignmentModel();
            patch.setDescription("Patched Description");
            
            MockMultipartFile file = new MockMultipartFile("file", "patch.pdf", "application/pdf", new byte[1024]);
            List<MultipartFile> files = Arrays.asList(file);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(existing);
            when(fileService.saveAssignmentFiles(anyList(), any(AssignmentModel.class), anyString()))
                .thenReturn(new ArrayList<>());
            
            // Act
            AssignmentModel result = assignmentService.patchAssignment("assignment-1", patch, files, null, false);
            
            // Assert
            assertNotNull(result);
            assertEquals("Patched Description", result.getDescription());
            verify(fileService, times(1)).saveAssignmentFiles(anyList(), any(AssignmentModel.class), anyString());
        }
        
        @Test
        @DisplayName("Patch assignment with file too large throws exception")
        void testPatchAssignment_FileTooLarge_ThrowsException() {
            // Arrange
            MockMultipartFile largeFile = new MockMultipartFile("file", "big.pdf", "application/pdf", 
                new byte[10 * 1024 * 1024]); // 10MB
            List<MultipartFile> files = Arrays.asList(largeFile);
            
            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> 
                assignmentService.patchAssignment("assignment-1", new AssignmentModel(), files, null, false));
            verify(assignmentRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Patch assignment does not change null fields")
        void testPatchAssignment_NullFields_NoChange() {
            // Arrange
            Date originalDueDate = new Date(System.currentTimeMillis() + 86400000);
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setName("Original Name");
            existing.setDescription("Original Description");
            existing.setVisible(true);
            existing.setDueDate(originalDueDate);
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            existing.setCourse(course);
            
            AssignmentModel patch = new AssignmentModel();
            // All fields are null except what we set
            patch.setVisible(false);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            when(assignmentRepository.save(any(AssignmentModel.class))).thenReturn(existing);
            
            // Act
            AssignmentModel result = assignmentService.patchAssignment("assignment-1", patch);
            
            // Assert
            assertEquals("Original Name", result.getName());
            assertEquals("Original Description", result.getDescription());
            assertEquals(originalDueDate, result.getDueDate());
            assertFalse(result.getVisible());
        }
        
        @Test
        @DisplayName("Unauthorized user cannot patch assignment")
        void testPatchAssignment_Unauthorized_ThrowsException() {
            // Arrange
            AssignmentModel existing = createValidAssignment();
            existing.setAssignmentId("assignment-1");
            existing.setCourse(createCourse("course-1"));
            
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(existing));
            doThrow(new UserNotAuthorizedException("Not authorized"))
                .when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            
            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> 
                assignmentService.patchAssignment("assignment-1", new AssignmentModel()));
            verify(assignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteAssignment Tests")
    class DeleteAssignmentTests {
        
        @Test
        @DisplayName("Admin deletes assignment successfully")
        void testDeleteAssignment_Admin_Success() {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            assignment.setAssignmentId("assignment-1");
            CourseModel course = createCourse("course-1");
            course.addInstructor(adminUser);
            assignment.setCourse(course);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(assignment));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            doNothing().when(assignmentRepository).deleteById("assignment-1");
            
            // Act
            assignmentService.deleteAssignment("assignment-1");
            
            // Assert
            verify(assignmentRepository, times(1)).deleteById("assignment-1");
        }
        
        @Test
        @DisplayName("Instructor deletes own course assignment")
        void testDeleteAssignment_InstructorOwnCourse_Success() {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            assignment.setAssignmentId("assignment-1");
            CourseModel course = createCourse("course-1");
            course.addInstructor(instructorUser);
            assignment.setCourse(course);
            
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(assignment));
            doNothing().when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            doNothing().when(assignmentRepository).deleteById("assignment-1");
            
            // Act
            assignmentService.deleteAssignment("assignment-1");
            
            // Assert
            verify(assignmentRepository, times(1)).deleteById("assignment-1");
        }
        
        @Test
        @DisplayName("Unauthorized user cannot delete assignment")
        void testDeleteAssignment_Unauthorized_ThrowsException() {
            // Arrange
            AssignmentModel assignment = createValidAssignment();
            assignment.setAssignmentId("assignment-1");
            assignment.setCourse(createCourse("course-1"));
            
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.findById("assignment-1")).thenReturn(Optional.of(assignment));
            doThrow(new UserNotAuthorizedException("Not authorized"))
                .when(courseService).userAllowedToEditCourse(any(CourseModel.class));
            
            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> 
                assignmentService.deleteAssignment("assignment-1"));
            verify(assignmentRepository, never()).deleteById(any());
        }
        
        @Test
        @DisplayName("Delete non-existent assignment throws exception")
        void testDeleteAssignment_NotFound_ThrowsException() {
            // Arrange
            when(assignmentRepository.findById("non-existent")).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThrows(AssignmentNotFoundException.class, () -> 
                assignmentService.deleteAssignment("non-existent"));
            verify(assignmentRepository, never()).deleteById(any());
        }
    }

    // Helper methods
    private AssignmentModel createValidAssignment() {
        AssignmentModel assignment = new AssignmentModel();
        assignment.setName("Test Assignment");
        assignment.setDescription("Test Description");
        assignment.setVisible(true);
        assignment.setDueDate(new Date(System.currentTimeMillis() + 86400000)); // Tomorrow
        assignment.setFiles(new ArrayList<>());
        return assignment;
    }

    private CourseModel createCourse(String id) {
        return new CourseModel(id, "Test Course", "CS101", null, null, null, null);
    }

    private void setupAssignmentsForUser(UserModel user, List<AssignmentModel> assignments, 
                                         List<CourseModel> courses, Role role) {
        if (role == Role.ADMIN) {
            when(assignmentRepository.findAll()).thenReturn(assignments);
        } else {
            for (CourseModel course : courses) {
                for (AssignmentModel assignment : assignments) {
                    course.addAssignment(assignment);
                }
                if (role == Role.INSTRUCTOR) {
                    course.addInstructor(user);
                    user.addTaughtCourse(course);
                } else {
                    user.addEnrollment(new EnrollmentModel("", null, user, course));
                }
            }
            when(courseService.getAllCourses()).thenReturn(courses);
        }
    }
}
