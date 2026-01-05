package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import peerport.backend.database.CoursesRepository;
import peerport.backend.exceptions.courses.CourseNotFoundException;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.*;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.service.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService Unit Tests")
public class CourseServiceTest {
    
    @InjectMocks
    private CourseService courseService;
    
    @Mock 
    private CoursesRepository coursesRepository;
    
    @Mock 
    private AuthService authService;
    
    @Mock 
    private FileService fileService;

    private static UserModel adminUser, instructorUser, studentUser;

    @BeforeAll
    static void initAll() {
        adminUser = new UserModel("admin", "admin", "admin", null, null, Role.ADMIN);
        instructorUser = new UserModel("instructor", "instructor", "instructor", null, null, Role.INSTRUCTOR);
        studentUser = new UserModel("student", "student", "student", null, null, Role.STUDENT);
    }

    @BeforeEach
    void setUp() {
        initAll();
        ReflectionTestUtils.setField(courseService, "fileUploadSizeLimit", 5242880L);
    }

    @Nested
    @DisplayName("getAllCourses Tests")
    class GetAllCoursesTests {
        @ParameterizedTest
        @MethodSource("provideRoleUsers")
        @DisplayName("{0} user retrieves courses successfully")
        void testGetAllCourses_Success(Role role, UserModel user) {
            // Arrange
            List<CourseModel> courses = Arrays.asList(new CourseModel(), new CourseModel());
            setupCoursesForUser(user, courses, role);
            when(authService.getCurrentUser()).thenReturn(user);

            // Act & Assert
            assertEquals(courses, courseService.getAllCourses());
        }

        @Test
        @DisplayName("User with no courses returns empty list")
        void testGetAllCourses_NoCourses() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(studentUser);

            // Act & Assert
            assertTrue(courseService.getAllCourses().isEmpty());
        }
    
        @Test
        @DisplayName("Unknown role treated as student")
        void testGetAllCourses_UnknownRole() {
            // Arrange
            UserModel unknownUser = new UserModel();
            List<CourseModel> courses = Arrays.asList(new CourseModel(), new CourseModel());
            enrollUserInCourse(unknownUser, courses.get(0));
            enrollUserInCourse(unknownUser, courses.get(1));
            when(authService.getCurrentUser()).thenReturn(unknownUser);

            // Act & Assert
            assertEquals(courses, courseService.getAllCourses());
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
    @DisplayName("getCourseById Tests")
    class GetCourseByIdTests {
        @ParameterizedTest
        @MethodSource("provideRoleUsers")
        @DisplayName("{0} gets course by id successfully")
        void testGetCourseById_Success(Role role, UserModel user) {
            // Arrange
            CourseModel course = new CourseModel();
            setupCourseAccessForUser(user, course, role);
            when(coursesRepository.findById("id-1")).thenReturn(Optional.of(course));
            when(authService.getCurrentUser()).thenReturn(user);

            // Act & Assert
            assertEquals(course, courseService.getCourseById("id-1"));
        }

        @Test
        @DisplayName("Course not found throws exception")
        void testGetCourseById_NotFound() {
            // Arrange
            when(coursesRepository.findById("id-1")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(CourseNotFoundException.class, () -> courseService.getCourseById("id-1"));
        }

        @Test
        @DisplayName("Unauthorized access throws exception")
        void testGetCourseById_Unauthorized() {
            // Arrange
            when(coursesRepository.findById("id-1")).thenReturn(Optional.of(new CourseModel()));
            when(authService.getCurrentUser()).thenReturn(studentUser);

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> courseService.getCourseById("id-1"));
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
    @DisplayName("createCourse Tests")
    class CreateCourseTests {
        @Test
        @DisplayName("Create course without image")
        void testCreateCourse_WithoutImage() throws IOException {
            // Arrange
            CourseModel course = new CourseModel(null, "Test", "CS101", null, null, null, null);
            CourseModel saved = new CourseModel("course-1", "Test", "CS101", null, null, null, null);
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.save(any(CourseModel.class))).thenReturn(saved);

            // Act
            CourseModel result = courseService.createCourse(course, null);

            // Assert
            assertEquals("course-1", result.getCourseId());
            verify(coursesRepository, times(1)).save(any(CourseModel.class));
            verify(fileService, never()).saveCourseImage(any(), anyString());
        }

        @Test
        @DisplayName("Instructor creates course")
        void testCreateCourse_ByInstructor() throws IOException {
            // Arrange
            CourseModel course = new CourseModel(null, "Test", "CS101", null, null, null, null);
            CourseModel saved = new CourseModel("course-1", "Test", "CS101", null, null, null, null);
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(coursesRepository.save(any(CourseModel.class))).thenReturn(saved);

            // Act
            CourseModel result = courseService.createCourse(course, null);

            // Assert
            assertEquals("course-1", result.getCourseId());
            verify(coursesRepository, times(1)).save(any(CourseModel.class));
        }

        @Test
        @DisplayName("Create course with image")
        void testCreateCourse_WithImage() throws IOException {
            // Arrange
            CourseModel course = new CourseModel(null, "Test", "CS101", null, null, null, null);
            CourseModel saved = new CourseModel("course-1", "Test", "CS101", null, null, null, null);
            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[1024]);
            FileModel file = new FileModel("image.jpg", "uploads/courses/course-1/image.jpg", "jpg", "image/jpeg");
            
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(coursesRepository.save(any(CourseModel.class))).thenReturn(saved);
            when(fileService.saveCourseImage(any(MultipartFile.class), eq("course-1"))).thenReturn(file);

            // Act
            CourseModel result = courseService.createCourse(course, image);

            // Assert
            assertEquals("course-1", result.getCourseId());
            verify(coursesRepository, times(2)).save(any(CourseModel.class));
            verify(fileService).saveCourseImage(any(MultipartFile.class), eq("course-1"));
        }

        @Test
        @DisplayName("Image too large throws exception")
        void testCreateCourse_ImageTooLarge() {
            // Arrange
            CourseModel course = new CourseModel(null, "Test", "CS101", null, null, null, null);
            MockMultipartFile largeImage = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[10 * 1024 * 1024]);

            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> courseService.createCourse(course, largeImage));
            verify(coursesRepository, never()).save(any(CourseModel.class));
        }

        @Test
        @DisplayName("IOException propagates")
        void testCreateCourse_IOException() throws IOException {
            // Arrange
            CourseModel course = new CourseModel(null, "Test", "CS101", null, null, null, null);
            CourseModel saved = new CourseModel("course-1", "Test", "CS101", null, null, null, null);
            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[1024]);
            
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(coursesRepository.save(any(CourseModel.class))).thenReturn(saved);
            when(fileService.saveCourseImage(any(), eq("course-1"))).thenThrow(new IOException("Fail"));

            // Act & Assert
            assertThrows(IOException.class, () -> courseService.createCourse(course, image));
        }

        @Test
        @DisplayName("Create course with all fields")
        void testCreateCourse_AllFields() throws IOException {
            // Arrange
            java.util.Date start = new java.util.Date();
            java.util.Date end = new java.util.Date();
            CourseModel course = new CourseModel(null, "Test", "CS101", true, "Description", start, end);
            CourseModel saved = new CourseModel("course-1", "Test", "CS101", true, "Description", start, end);
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.save(any(CourseModel.class))).thenReturn(saved);

            // Act
            CourseModel result = courseService.createCourse(course, null);

            // Assert
            assertEquals("course-1", result.getCourseId());
            assertEquals("Description", result.getDescription());
        }
    }

    @Nested
    @DisplayName("updateCourse Tests")
    class UpdateCourseTests {
        @Test
        @DisplayName("Admin updates course successfully")
        void testUpdateCourse_Admin() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            CourseModel updated = new CourseModel(null, "New", "CS102", null, null, null, null);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(coursesRepository.save(any())).thenReturn(existing);

            // Act
            CourseModel result = courseService.updateCourse("course-1", updated);

            // Assert
            assertEquals("New", result.getName());
            assertEquals("CS102", result.getCourseCode());
            verify(coursesRepository).save(existing);
        }

        @Test
        @DisplayName("Update course with all fields")
        void testUpdateCourse_AllFields() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            existing.setDescription("Old desc");
            existing.setIsOpen(false);
            java.util.Date newStart = new java.util.Date();
            java.util.Date newEnd = new java.util.Date();
            CourseModel updated = new CourseModel(null, "New", "CS102", true, "New desc", newStart, newEnd);
            updated.setIsOpen(true);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(coursesRepository.save(any())).thenReturn(existing);

            // Act
            CourseModel result = courseService.updateCourse("course-1", updated);

            // Assert
            assertEquals("New", result.getName());
            assertEquals("CS102", result.getCourseCode());
            assertEquals("New desc", result.getDescription());
            assertTrue(result.getIsOpen());
            assertNotNull(result.getStartDate());
            assertNotNull(result.getEndDate());
        }

        @Test
        @DisplayName("Instructor updates own course")
        void testUpdateCourse_Instructor() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            existing.addInstructor(instructorUser);
            CourseModel updated = new CourseModel(null, "New", "CS102", null, null, null, null);
            
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(coursesRepository.save(any())).thenReturn(existing);

            // Act
            CourseModel result = courseService.updateCourse("course-1", updated);

            // Assert
            assertEquals("New", result.getName());
            verify(coursesRepository).save(existing);
        }

        @Test
        @DisplayName("Unauthorized user throws exception")
        void testUpdateCourse_Unauthorized() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            CourseModel updated = new CourseModel(null, "New", "CS102", null, null, null, null);
            
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> courseService.updateCourse("course-1", updated));
            verify(coursesRepository, never()).save(any());
        }

        @Test
        @DisplayName("Course not found throws exception")
        void testUpdateCourse_NotFound() {
            // Arrange
            when(coursesRepository.findById("course-1")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(CourseNotFoundException.class, () -> courseService.updateCourse("course-1", new CourseModel()));
        }

        @Test
        @DisplayName("Update with image")
        void testUpdateCourse_WithImage() throws IOException {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            CourseModel updated = new CourseModel(null, "New", "CS102", null, null, null, null);
            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[1024]);
            FileModel file = new FileModel("image.jpg", "uploads/courses/course-1/image.jpg", "jpg", "image/jpeg");
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(coursesRepository.save(any())).thenReturn(existing);
            when(fileService.saveCourseImage(any(), eq("course-1"))).thenReturn(file);

            // Act
            courseService.updateCourse("course-1", updated, image);

            // Assert
            verify(fileService).saveCourseImage(any(), eq("course-1"));
            verify(coursesRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Image too large throws exception")
        void testUpdateCourse_ImageTooLarge() {
            // Arrange
            MockMultipartFile largeImage = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[10 * 1024 * 1024]);

            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> courseService.updateCourse("course-1", new CourseModel(), largeImage));
        }

        @Test
        @DisplayName("IOException propagates in update")
        void testUpdateCourse_IOException() throws IOException {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            CourseModel updated = new CourseModel(null, "New", "CS102", null, null, null, null);
            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[1024]);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(fileService.saveCourseImage(any(), eq("course-1"))).thenThrow(new IOException("Fail"));

            // Act & Assert
            assertThrows(IOException.class, () -> courseService.updateCourse("course-1", updated, image));
        }
    }

    @Nested
    @DisplayName("patchCourse Tests")
    class PatchCourseTests {
        @Test
        @DisplayName("Patch updates only non-null fields")
        void testPatchCourse_OnlyNonNull() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            existing.setDescription("Old desc");
            CourseModel patch = new CourseModel();
            patch.setName("New");
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(coursesRepository.save(any())).thenReturn(existing);

            // Act
            CourseModel result = courseService.patchCourse("course-1", patch);

            // Assert
            assertEquals("New", result.getName());
            assertEquals("Old desc", result.getDescription());
            assertEquals("CS101", result.getCourseCode());
        }

        @Test
        @DisplayName("Patch updates multiple fields")
        void testPatchCourse_MultipleFields() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            existing.setDescription("Old desc");
            existing.setIsOpen(false);
            CourseModel patch = new CourseModel();
            patch.setName("New");
            patch.setCourseCode("CS102");
            patch.setIsOpen(true);
            patch.setDescription("New desc");
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(coursesRepository.save(any())).thenReturn(existing);

            // Act
            CourseModel result = courseService.patchCourse("course-1", patch);

            // Assert
            assertEquals("New", result.getName());
            assertEquals("CS102", result.getCourseCode());
            assertEquals("New desc", result.getDescription());
            assertTrue(result.getIsOpen());
        }

        @Test
        @DisplayName("Patch course by CourseModel directly")
        void testPatchCourse_ByModel() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            existing.setDescription("Old desc");
            CourseModel patch = new CourseModel();
            patch.setName("New");
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.save(any())).thenReturn(existing);

            // Act
            CourseModel result = courseService.patchCourse(existing, patch);

            // Assert
            assertEquals("New", result.getName());
            assertEquals("Old desc", result.getDescription());
        }

        @Test
        @DisplayName("Instructor patches own course")
        void testPatchCourse_InstructorOwn() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            existing.addInstructor(instructorUser);
            CourseModel patch = new CourseModel();
            patch.setName("New");
            
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(coursesRepository.save(any())).thenReturn(existing);

            // Act
            CourseModel result = courseService.patchCourse("course-1", patch);

            // Assert
            assertEquals("New", result.getName());
            verify(coursesRepository).save(existing);
        }

        @Test
        @DisplayName("Unauthorized user throws exception")
        void testPatchCourse_Unauthorized() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> courseService.patchCourse("course-1", new CourseModel()));
        }

        @Test
        @DisplayName("Course not found throws exception")
        void testPatchCourse_NotFound() {
            // Arrange
            when(coursesRepository.findById("course-1")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(CourseNotFoundException.class, () -> courseService.patchCourse("course-1", new CourseModel()));
        }

        @Test
        @DisplayName("Patch with image")
        void testPatchCourse_WithImage() throws IOException {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            CourseModel patch = new CourseModel();
            patch.setName("New");
            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[1024]);
            FileModel file = new FileModel("image.jpg", "uploads/courses/course-1/image.jpg", "jpg", "image/jpeg");
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(coursesRepository.save(any())).thenReturn(existing);
            when(fileService.saveCourseImage(any(), eq("course-1"))).thenReturn(file);

            // Act
            courseService.patchCourse("course-1", patch, image);

            // Assert
            verify(fileService).saveCourseImage(any(), eq("course-1"));
            verify(coursesRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Patch with dates")
        void testPatchCourse_WithDates() {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            CourseModel patch = new CourseModel();
            patch.setName("New");
            patch.setStartDate(new java.util.Date());
            patch.setEndDate(new java.util.Date());
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(coursesRepository.save(any())).thenReturn(existing);

            // Act
            CourseModel result = courseService.patchCourse("course-1", patch);

            // Assert
            assertNotNull(result.getStartDate());
            assertNotNull(result.getEndDate());
            verify(coursesRepository).save(existing);
        }

        @Test
        @DisplayName("Image too large throws exception")
        void testPatchCourse_ImageTooLarge() {
            // Arrange
            MockMultipartFile largeImage = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[10 * 1024 * 1024]);

            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> courseService.patchCourse("course-1", new CourseModel(), largeImage));
        }

        @Test
        @DisplayName("IOException propagates in patch")
        void testPatchCourse_IOException() throws IOException {
            // Arrange
            CourseModel existing = createCourse("course-1", "Old", "CS101");
            CourseModel patch = new CourseModel();
            patch.setName("New");
            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[1024]);
            
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(existing));
            when(fileService.saveCourseImage(any(), eq("course-1"))).thenThrow(new IOException("Fail"));

            // Act & Assert
            assertThrows(IOException.class, () -> courseService.patchCourse("course-1", patch, image));
        }
    }

    @Nested
    @DisplayName("deleteCourse Tests")
    class DeleteCourseTests {
        @Test
        @DisplayName("Admin deletes course")
        void testDeleteCourse_Admin() {
            // Arrange
            CourseModel course = createCourse("course-1", "Test", "CS101");
            when(authService.getCurrentUser()).thenReturn(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(course));
            doNothing().when(coursesRepository).deleteById("course-1");

            // Act
            courseService.deleteCourse("course-1");

            // Assert
            verify(coursesRepository).deleteById("course-1");
        }

        @Test
        @DisplayName("Instructor deletes own course")
        void testDeleteCourse_Instructor() {
            // Arrange
            CourseModel course = createCourse("course-1", "Test", "CS101");
            course.addInstructor(instructorUser);
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(course));
            doNothing().when(coursesRepository).deleteById("course-1");

            // Act
            courseService.deleteCourse("course-1");

            // Assert
            verify(coursesRepository).deleteById("course-1");
        }

        @Test
        @DisplayName("Unauthorized user throws exception")
        void testDeleteCourse_Unauthorized() {
            // Arrange
            CourseModel course = createCourse("course-1", "Test", "CS101");
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(course));

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> courseService.deleteCourse("course-1"));
            verify(coursesRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Course not found throws exception")
        void testDeleteCourse_NotFound() {
            // Arrange
            when(coursesRepository.findById("course-1")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(CourseNotFoundException.class, () -> courseService.deleteCourse("course-1"));
        }
    }

    // Helper methods
    private void setupCoursesForUser(UserModel user, List<CourseModel> courses, Role role) {
        if (role == Role.ADMIN) {
            when(coursesRepository.findAll()).thenReturn(courses);
        } else if (role == Role.INSTRUCTOR) {
            for (int i = 0; i < courses.size(); i++) {
                if (i % 2 == 0) {
                    courses.get(i).addInstructor(user);
                    user.addTaughtCourse(courses.get(i));
                } else {
                    enrollUserInCourse(user, courses.get(i));
                }
            }
        } else {
            courses.forEach(course -> enrollUserInCourse(user, course));
        }
    }

    private void setupCourseAccessForUser(UserModel user, CourseModel course, Role role) {
        if (role == Role.INSTRUCTOR) {
            course.addInstructor(user);
            user.addTaughtCourse(course);
        } else if (role == Role.STUDENT) {
            enrollUserInCourse(user, course);
        }
    }

    private void enrollUserInCourse(UserModel user, CourseModel course) {
        user.addEnrollment(new EnrollmentModel("", null, user, course));
    }

    private CourseModel createCourse(String id, String name, String code) {
        return new CourseModel(id, name, code, null, null, null, null);
    }
}