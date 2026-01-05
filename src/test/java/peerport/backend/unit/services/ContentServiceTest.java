package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import peerport.backend.model.ContentModel;
import peerport.backend.model.CourseModel;
import peerport.backend.model.EnrollmentModel;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.database.ContentRepository;
import peerport.backend.database.CoursesRepository;
import peerport.backend.dto.content.ContentWithChildrenDTO;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.exceptions.content.ContentNotFoundException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.UserModel;
import peerport.backend.service.AuthService;
import peerport.backend.service.ContentService;
import peerport.backend.service.CourseService;
import peerport.backend.service.FileService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.ArgumentMatchers.any;
import peerport.backend.model.FileModel;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentService Unit Tests")
public class ContentServiceTest {
    
    @InjectMocks 
    private ContentService contentService;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private AuthService authService;

    @Mock
    private FileService fileService;

    @Mock
    private CoursesRepository coursesRepository;

    @Spy
    private CourseService courseService;

    private Validator validator;

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
    void setUp() {
        makeUsers();

        // Initialize a validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // Inject the validator into the contentService
        try {
            Field validatorField = ContentService.class.getDeclaredField("validator");
            validatorField.setAccessible(true);
            validatorField.set(contentService, validator);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject validator into ContentService.java", e);
        }

        // Inject the authService and courseRepository into the courseService (for real implementation)
        try {
            Field authServiceField = CourseService.class.getDeclaredField("authService");
            authServiceField.setAccessible(true);
            authServiceField.set(courseService, authService);
            
            Field courseRepositoryField = CourseService.class.getDeclaredField("courseRepository");
            courseRepositoryField.setAccessible(true);
            courseRepositoryField.set(courseService, coursesRepository);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject dependencies into CourseService.java", e);
        }
    }

    @Nested
    @DisplayName("validateContent Tests")
    class ValidateContentTests {
        @Test
        @DisplayName("Should throw FailedToParseFormDataException when content is null")
        void testValidateContent_NullContent_ThrowsException() {
            // Act & Assert
            FailedToParseFormDataException exception = assertThrows(FailedToParseFormDataException.class, () -> {
                contentService.validateContent(null);
            });

            // Assert
            assertTrue(exception.getMessage().equals("Content data is required."));
        }

        @Test
        @DisplayName("Should throw FailedToParseFormDataException when content is invalid")
        void testValidateContent_InvalidContent_ThrowsException() {
            // Arrange
            ContentModel invalidContent = new ContentModel();
            
            // Act & Assert
            FailedToParseFormDataException exception = assertThrows(FailedToParseFormDataException.class, () -> {
                contentService.validateContent(invalidContent);
            });

            // Assert
            assertTrue(exception.getMessage().startsWith("Content data validation failed: "));
        }

        @Test
        @DisplayName("Should not throw any exception when content is valid")
        void testValidateContent_ValidContent_NoException() {
            // Arrange
            ContentModel validContent = new ContentModel();
            validContent.setTitle("Valid Title");
            validContent.setDescription("Valid Description");
            validContent.setVisible(true);

            // Act & Assert
            assertDoesNotThrow(() -> {
                contentService.validateContent(validContent);
            });
        }
    }

    @Nested
    @DisplayName("getAllContent Tests")
    class GetAllContentTests {
        @ParameterizedTest
        @MethodSource("provideRoleUsers")
        @DisplayName("{0} user should be able to get content successfully")
        void testGetAllContent_ByRoleUsers(Role role, UserModel user) {
            // Arrange
            List<ContentModel> contentList = List.of(
                new ContentModel(),
                new ContentModel()
            );
            setupContentForUser(user, contentList, role);
            when(authService.getCurrentUser()).thenReturn(user);

            // Act
            List<ContentModel> result = contentService.getAllContent();

            // Assert
            assertTrue(result.size() == contentList.size(),
                String.format("Expected content list size %d, but got %d", contentList.size(), result.size())
            );
            assertTrue(
                contentList.containsAll(result),
                String.format("Expected content list (%d): %s, but got (%d): %s", contentList.size(), contentList, result.size(), result)
            );
        }

        @Test
        @DisplayName("Should return empty list when no content is available")
        void testGetAllContent_NoContent_ReturnsEmptyList() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(studentUser);

            // Act & Assert
            assertTrue(
                contentService.getAllContent().isEmpty(),
                "Expected empty content list, but got non-empty list."
            );
        }

        @Test
        @DisplayName("Unknown role is treated as student")
        void testGetAllContent_UnknownRole_TreatedAsStudent() {
            // Arrange
            UserModel unknownRoleUser = new UserModel();
            List<ContentModel> contentList = List.of(new ContentModel(), new ContentModel());
            setupContentForUser(unknownRoleUser, contentList.get(0));
            setupContentForUser(unknownRoleUser, contentList.get(1));
            when(authService.getCurrentUser()).thenReturn(unknownRoleUser);

            // Act
            List<ContentModel> result = contentService.getAllContent();

            // Assert
            assertTrue(result.size() == contentList.size(),
                String.format("Expected content list size %d, but got %d", contentList.size(), result.size())
            );
            assertTrue(
                contentList.containsAll(result),
                String.format("Expected content list (%d): %s, but got (%d): %s", contentList.size(), contentList, result.size(), result)
            );
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
    @DisplayName("getStructuredContent Tests")
    class GetStructuredContentTests {
        @ParameterizedTest
        @MethodSource("provideRoleUsers")
        @DisplayName("{0} user should be able to get structured content successfully")
        void testGetStructuredContent_ByRoleUsers(Role role, UserModel user) {
            // Arrange
            List<ContentModel> contentList = List.of(
                new ContentModel("C1", "Title 1", "Desc 1", true, null, null, null, null, null, null),
                new ContentModel("C2", "Title 2", "Desc 2", true, null, null, null, null, null, null)
            );
            setupContentForUser(user, contentList, role);
            when(authService.getCurrentUser()).thenReturn(user);

            // Act
            List<ContentWithChildrenDTO> result = contentService.getStructuredContent();

            // Assert
            assertTrue(result.size() == contentList.size(),
                String.format("Expected content list size %d, but got %d", contentList.size(), result.size())
            );
            assertAll(
                () -> assertTrue(List.of("C1", "C2").contains(result.get(0).contentId)),
                () -> assertTrue(List.of("C1", "C2").contains(result.get(1).contentId))
            );
        }

        @Test
        @DisplayName("Should return empty list when no content is available")
        void testGetStructuredContent_NoContent_ReturnsEmptyList() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(studentUser);

            // Act & Assert
            assertTrue(
                contentService.getStructuredContent().isEmpty(),
                "Expected empty content list, but got non-empty list."
            );
        }

        @Test
        @DisplayName("Unknown role is treated as student")
        void testGetStructuredContent_UnknownRole_TreatedAsStudent() {
            // Arrange
            UserModel unknownRoleUser = new UserModel();
            List<ContentModel> contentList = List.of(
                new ContentModel("C1", "Title 1", "Desc 1", true, null, null, null, null, null, null),
                new ContentModel("C2", "Title 2", "Desc 2", true, null, null, null, null, null, null)
            );
            setupContentForUser(unknownRoleUser, contentList.get(0));
            setupContentForUser(unknownRoleUser, contentList.get(1));
            when(authService.getCurrentUser()).thenReturn(unknownRoleUser);

            // Act
            List<ContentWithChildrenDTO> result = contentService.getStructuredContent();

            // Assert
            assertTrue(result.size() == contentList.size(),
                String.format("Expected content list size %d, but got %d", contentList.size(), result.size())
            );
            assertAll(
                () -> assertTrue(List.of("C1", "C2").contains(result.get(0).contentId)),
                () -> assertTrue(List.of("C1", "C2").contains(result.get(1).contentId))
            );
        }

        @Test
        @DisplayName("Should return structured content with sub-content nested correctly")
        void testGetStructuredContent_WithSubContent_ReturnsNestedStructure() {
            // Arrange
            UserModel testUser = new UserModel();
            
            ContentModel parentContent = new ContentModel("P1", "Parent Title", "Parent Description", true, null, null, null, null, new ArrayList<>(), null);
            ContentModel subContent1 = new ContentModel("S1", "Sub Title 1", "Sub Description 1", true, null, null, null, parentContent, null, null);
            ContentModel subContent2 = new ContentModel("S2", "Sub Title 2", "Sub Description 2", true, null, null, null, parentContent, null, null);
            
            parentContent.setSubContent(List.of(subContent1, subContent2));
            
            // Setup content for user
            setupContentForUser(testUser, parentContent);
            setupContentForUser(testUser, subContent1);
            setupContentForUser(testUser, subContent2);
            when(authService.getCurrentUser()).thenReturn(testUser);

            // Act
            List<ContentWithChildrenDTO> result = contentService.getStructuredContent();

            // Assert
            assertTrue(result.size() >= 1,
                "Expected at least 1 content item, but got " + result.size()
            );
            
            // Find the parent content in results
            final ContentWithChildrenDTO parentResult = result.stream()
                .filter(c -> "P1".equals(c.contentId))
                .findFirst()
                .orElse(null);
            
            assertTrue(parentResult != null, "Parent content with ID 'P1' should be present in results");
            assertTrue(parentResult.subContent != null, "Parent content should have subContent list");
            assertTrue(parentResult.subContent.size() == 2,
                String.format("Expected 2 sub-contents, but got %d", parentResult.subContent.size())
            );
            assertAll(
                () -> assertTrue(List.of("S1", "S2").contains(parentResult.subContent.get(0).contentId)),
                () -> assertTrue(List.of("S1", "S2").contains(parentResult.subContent.get(1).contentId))
            );
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
    @DisplayName("getContentById Tests")
    class GetContentByIdTests {

        @ParameterizedTest
        @MethodSource("provideRoleUsers")
        @DisplayName("{0} user should be able to get content by ID successfully")
        void testGetContentById_ByRoleUsers(Role role, UserModel user) {
            // Arrange
            ContentModel content = new ContentModel();
            setupContentForUser(user, content);
            when(contentRepository.findById("id-1")).thenReturn(Optional.of(content));
            when(authService.getCurrentUser()).thenReturn(user);

            // Act & Assert
            assertEquals(content, contentService.getContentById("id-1"));
        }

        @Test
        @DisplayName("Should throw exception when content ID does not exist")
        void testGetContentById_NonExistentId_ThrowsException() {
            // Arrange
            when(contentRepository.findById("id-1")).thenReturn(Optional.empty());

            // Act & Assert
            ContentNotFoundException exception = assertThrows(ContentNotFoundException.class, () -> {
                contentService.getContentById("id-1");
            });

            String exceptionMessage = exception.getMessage().toLowerCase();
            assertTrue(exceptionMessage.contains("content") && exceptionMessage.contains("not found"));
        }

        @Test
        @DisplayName("Unauthorized access throws UserNotAuthorizedException")
        void testGetContentById_UnauthorizedAccess_ThrowsException() {
            // Arrange
            ContentModel content = new ContentModel();
            setupContentForUser(adminUser, content);
            when(contentRepository.findById("id-1")).thenReturn(Optional.of(content));
            when(authService.getCurrentUser()).thenReturn(new UserModel()); // User with no access

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> {
                contentService.getContentById("id-1");
            });
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
    @DisplayName("createContent Tests")
    class CreateContentTests {
        @Test
        @DisplayName("Should throw FailedToParseFormDataException when content is null")
        void testCreateContent_NullContent_ThrowsException() {
            // Act & Assert
            FailedToParseFormDataException exception = assertThrows(FailedToParseFormDataException.class, () -> {
                contentService.validateContent(null);
            });
            assertEquals("Content data is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should validate content successfully when title is valid")
        void testCreateContent_ValidContent_NoException() {
            // Arrange
            ContentModel content = new ContentModel();
            content.setTitle("Valid Title");
            content.setDescription("Valid Description");

            // Act & Assert
            assertDoesNotThrow(() -> {
                contentService.validateContent(content);
            });
        }

        @Test
        @DisplayName("Should create content successfully without files")
        void testCreateContent_NoFiles_Success() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel content = new ContentModel();
            content.setTitle("Content without Files");
            content.setDescription("Description");
            content.setVisible(true);
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(course));
            when(contentRepository.save(content)).thenReturn(content);

            // Act
            ContentModel result = contentService.createContent(content, "course-1");

            // Assert
            assertNotNull(result);
            assertEquals("Content without Files", result.getTitle());
            assertEquals("Description", result.getDescription());
            assertTrue(result.getVisible());
        }

        @Test
        @DisplayName("Should create content with files successfully")
        void testCreateContent_WithFiles_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel content = new ContentModel();
            content.setTitle("Content with Files");
            ArrayList<FileModel> files = new ArrayList<>();
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            when(coursesRepository.findById("course-1")).thenReturn(Optional.of(course));
            when(contentRepository.save(content)).thenReturn(content);
            when(fileService.saveContentFiles(any(), any(), any())).thenReturn(files);

            // Create mock multipart files
            List<MultipartFile> multipartFiles = new ArrayList<>();
            MockMultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
            multipartFiles.add(mockFile);

            // Act
            ContentModel result = contentService.createContent(content, "course-1", multipartFiles);

            // Assert
            assertNotNull(result);
            assertEquals("Content with Files", result.getTitle());
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
    @DisplayName("updateContent Tests")
    class UpdateContentTests {
        @Test
        @DisplayName("Should update content successfully by authorized user")
        void testUpdateContent_AuthorizedUser_Success() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Old Title");
            existingContent.setDescription("Old Description");
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            
            ContentModel updatedContent = new ContentModel();
            updatedContent.setTitle("New Title");
            updatedContent.setDescription("New Description");

            // Act
            ContentModel result = contentService.updateContent(existingContent, updatedContent);

            // Assert
            assertNotNull(result);
            assertEquals("New Title", result.getTitle());
            assertEquals("New Description", result.getDescription());
        }

        @Test
        @DisplayName("Should throw UserNotAuthorizedException when user not authorized")
        void testUpdateContent_UnauthorizedUser_ThrowsException() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(studentUser);
            ContentModel existingContent = new ContentModel();
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addContent(existingContent);
            existingContent.setCourse(course);

            // Act & Assert
            UserNotAuthorizedException exception = assertThrows(UserNotAuthorizedException.class, () -> {
                contentService.updateContent(existingContent, new ContentModel());
            });
            assertNotNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should update content by contentId successfully")
        void testUpdateContent_ByCourseId_Success() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Old Title");
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            
            ContentModel updatedContent = new ContentModel();
            updatedContent.setTitle("Updated Title");

            // Act
            ContentModel result = contentService.updateContent("content-1", updatedContent);

            // Assert
            assertNotNull(result);
            assertEquals("Updated Title", result.getTitle());
        }

        @Test
        @DisplayName("Should update content with files successfully")
        void testUpdateContent_WithFiles_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Old Title");
            existingContent.setFiles(new ArrayList<>());
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            when(fileService.saveContentFiles(any(), any(), any())).thenReturn(new ArrayList<>());
            
            ContentModel updatedContent = new ContentModel();
            updatedContent.setTitle("Updated Title");
            
            List<MultipartFile> files = new ArrayList<>();
            files.add(new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes()));
            List<String> removeFileIds = new ArrayList<>();

            // Act
            ContentModel result = contentService.updateContent("content-1", updatedContent, files, removeFileIds, false);

            // Assert
            assertNotNull(result);
            assertEquals("Updated Title", result.getTitle());
        }

        @Test
        @DisplayName("Should update content and replace all files when replaceAll is true")
        void testUpdateContent_ReplaceAllFiles_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Old Title");
            FileModel oldFile = new FileModel("old-file.txt", "/path", "txt", "text/plain");
            ArrayList<FileModel> existingFiles = new ArrayList<>();
            existingFiles.add(oldFile);
            existingContent.setFiles(existingFiles);
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            when(fileService.saveContentFiles(any(), any(), any())).thenReturn(new ArrayList<>());
            
            ContentModel updatedContent = new ContentModel();
            updatedContent.setTitle("Updated Title");
            
            List<MultipartFile> newFiles = new ArrayList<>();
            newFiles.add(new MockMultipartFile("file", "new.txt", "text/plain", "new".getBytes()));

            // Act
            ContentModel result = contentService.updateContent("content-1", updatedContent, newFiles, null, true);

            // Assert
            assertNotNull(result);
            assertEquals("Updated Title", result.getTitle());
            verify(fileService, times(1)).deleteFile(oldFile);
        }

        @Test
        @DisplayName("Should update content with selective file removal")
        void testUpdateContent_SelectiveRemove_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            
            FileModel file1 = new FileModel("file1.txt", "/path1", "txt", "text/plain");
            FileModel file2 = new FileModel("file2.txt", "/path2", "txt", "text/plain");
            
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Content");
            existingContent.setFiles(new ArrayList<>(Arrays.asList(file1, file2)));
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            when(fileService.saveContentFiles(any(), any(), any())).thenReturn(new ArrayList<>());
            
            ContentModel updatedContent = new ContentModel();
            List<String> removeFileIds = new ArrayList<>();
            removeFileIds.add(file1.getFileId());
            
            // Act
            ContentModel result = contentService.updateContent("content-1", updatedContent, new ArrayList<>(), removeFileIds, false);

            // Assert
            assertNotNull(result);
            verify(fileService, times(1)).deleteFile(file1);
        }

        @Test
        @DisplayName("Should update content when files list is initially null")
        void testUpdateContent_NullFilesList_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Content");
            existingContent.setFiles(null); // No files initially
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            when(fileService.saveContentFiles(any(), any(), any())).thenReturn(new ArrayList<>());
            
            ContentModel updatedContent = new ContentModel();
            updatedContent.setTitle("Updated");
            
            List<MultipartFile> newFiles = new ArrayList<>();
            newFiles.add(new MockMultipartFile("file", "new.txt", "text/plain", "new".getBytes()));
            
            // Act
            ContentModel result = contentService.updateContent("content-1", updatedContent, newFiles, null, false);

            // Assert
            assertNotNull(result);
            assertEquals("Updated", result.getTitle());
        }

        @Test
        @DisplayName("Should remove some files but keep others when selective removal requested")
        void testUpdateContent_PartialFileRemoval_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            
            FileModel file1 = new FileModel("file1.txt", "/path1", "txt", "text/plain");
            FileModel file2 = new FileModel("file2.txt", "/path2", "txt", "text/plain");
            FileModel file3 = new FileModel("file3.txt", "/path3", "txt", "text/plain");
            
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Content");
            existingContent.setFiles(new ArrayList<>(Arrays.asList(file1, file2, file3)));
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            when(fileService.saveContentFiles(any(), any(), any())).thenReturn(new ArrayList<>());
            
            ContentModel updatedContent = new ContentModel();
            List<String> removeFileIds = new ArrayList<>();
            removeFileIds.add(file1.getFileId());
            removeFileIds.add(file2.getFileId());
            // file3 should be kept
            
            // Act
            ContentModel result = contentService.updateContent("content-1", updatedContent, new ArrayList<>(), removeFileIds, false);

            // Assert
            assertNotNull(result);
            verify(fileService, times(1)).deleteFile(file1);
            verify(fileService, times(1)).deleteFile(file2);
        }

        @Test
        @DisplayName("Should keep files not in removal list")
        void testUpdateContent_KeepSomeFilesOnRemoval_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            
            FileModel file1 = new FileModel("file1.txt", "/path1", "txt", "text/plain");
            FileModel file2 = new FileModel("file2.txt", "/path2", "txt", "text/plain");
            
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Content");
            existingContent.setFiles(new ArrayList<>(Arrays.asList(file1, file2)));
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            
            ContentModel updatedContent = new ContentModel();
            List<String> removeFileIds = new ArrayList<>();
            removeFileIds.add(file1.getFileId());
            // file2 should be kept
            
            // Act
            ContentModel result = contentService.updateContent("content-1", updatedContent, new ArrayList<>(), removeFileIds, false);

            // Assert
            assertNotNull(result);
            verify(fileService, times(1)).deleteFile(file1);
            // Verify the update was saved at least once
            verify(contentRepository, times(2)).save(existingContent);
        }

        @Test
        @DisplayName("Should add new files without removing existing ones")
        void testUpdateContent_AddFilesOnly_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            
            FileModel existingFile = new FileModel("existing.txt", "/path", "txt", "text/plain");
            
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Content");
            existingContent.setFiles(new ArrayList<>(List.of(existingFile)));
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            FileModel newFileModel = new FileModel("new.txt", "/path", "txt", "text/plain");
            List<FileModel> newFilesList = new ArrayList<>(List.of(newFileModel));
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            when(fileService.saveContentFiles(any(), any(), any())).thenReturn(newFilesList);
            
            ContentModel updatedContent = new ContentModel();
            
            List<MultipartFile> newFiles = new ArrayList<>();
            newFiles.add(new MockMultipartFile("file", "new.txt", "text/plain", "new".getBytes()));
            
            // Act
            ContentModel result = contentService.updateContent("content-1", updatedContent, newFiles, null, false);

            // Assert
            assertNotNull(result);
            verify(fileService, times(1)).saveContentFiles(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("patchContent Tests")
    class PatchContentTests {
        @Test
        @DisplayName("Should patch only title when other fields are null")
        void testPatchContent_PartialUpdate_Success() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Old Title");
            existingContent.setDescription("Description");
            existingContent.setVisible(true);
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            
            ContentModel patchedContent = new ContentModel();
            patchedContent.setTitle("New Title");
            // Other fields are null

            // Act
            ContentModel result = contentService.patchContent(existingContent, patchedContent);

            // Assert
            assertNotNull(result);
            assertEquals("New Title", result.getTitle());
            assertEquals("Description", result.getDescription());
            assertTrue(result.getVisible());
        }

        @Test
        @DisplayName("Should patch content by contentId successfully")
        void testPatchContent_ByContentId_Success() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Old Title");
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            
            ContentModel patchedContent = new ContentModel();
            patchedContent.setDescription("New Description");

            // Act
            ContentModel result = contentService.patchContent("content-1", patchedContent);

            // Assert
            assertNotNull(result);
            assertEquals("Old Title", result.getTitle());
            assertEquals("New Description", result.getDescription());
        }

        @Test
        @DisplayName("Should throw UserNotAuthorizedException when user not authorized")
        void testPatchContent_UnauthorizedUser_ThrowsException() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(studentUser);
            ContentModel existingContent = new ContentModel();
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addContent(existingContent);
            existingContent.setCourse(course);

            // Act & Assert
            UserNotAuthorizedException exception = assertThrows(UserNotAuthorizedException.class, () -> {
                contentService.patchContent(existingContent, new ContentModel());
            });
            assertNotNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should patch content with files successfully")
        void testPatchContent_WithFiles_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Old Title");
            existingContent.setDescription("Description");
            existingContent.setVisible(true);
            existingContent.setFiles(new ArrayList<>());
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            when(fileService.saveContentFiles(any(), any(), any())).thenReturn(new ArrayList<>());
            
            ContentModel patchedContent = new ContentModel();
            patchedContent.setTitle("New Title");
            
            List<MultipartFile> files = new ArrayList<>();
            files.add(new MockMultipartFile("file", "patched.txt", "text/plain", "patched".getBytes()));

            // Act
            ContentModel result = contentService.patchContent("content-1", patchedContent, files);

            // Assert
            assertNotNull(result);
            assertEquals("New Title", result.getTitle());
            assertEquals("Description", result.getDescription());
        }

        @Test
        @DisplayName("Should patch content with files but keep existing fields")
        void testPatchContent_PartialWithFiles_Success() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            existingContent.setTitle("Original Title");
            existingContent.setDescription("Original Description");
            existingContent.setVisible(false);
            existingContent.setFiles(new ArrayList<>());
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));
            when(contentRepository.save(existingContent)).thenReturn(existingContent);
            when(fileService.saveContentFiles(any(), any(), any())).thenReturn(new ArrayList<>());
            
            ContentModel patchedContent = new ContentModel();
            patchedContent.setVisible(true); // Only update visible, not title or description
            
            List<MultipartFile> files = new ArrayList<>();
            files.add(new MockMultipartFile("file", "patch.txt", "text/plain", "patch".getBytes()));

            // Act
            ContentModel result = contentService.patchContent("content-1", patchedContent, files);

            // Assert
            assertNotNull(result);
            assertEquals("Original Title", result.getTitle());
            assertEquals("Original Description", result.getDescription());
            assertTrue(result.getVisible());
        }
    }

    @Nested
    @DisplayName("deleteContent Tests")
    class DeleteContentTests {
        @Test
        @DisplayName("Should delete content successfully by authorized user")
        void testDeleteContent_AuthorizedUser_Success() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            ContentModel existingContent = new ContentModel();
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));

            // Act & Assert
            assertDoesNotThrow(() -> {
                contentService.deleteContent("content-1");
            });
        }

        @Test
        @DisplayName("Should throw UserNotAuthorizedException when user not authorized")
        void testDeleteContent_UnauthorizedUser_ThrowsException() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(studentUser);
            ContentModel existingContent = new ContentModel();
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));

            // Act & Assert
            UserNotAuthorizedException exception = assertThrows(UserNotAuthorizedException.class, () -> {
                contentService.deleteContent("content-1");
            });
            assertNotNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should allow instructor to delete content from their course")
        void testDeleteContent_InstructorUser_Success() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            ContentModel existingContent = new ContentModel();
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(instructorUser);
            instructorUser.addTaughtCourse(course);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));

            // Act & Assert
            assertDoesNotThrow(() -> {
                contentService.deleteContent("content-1");
            });
        }

        @Test
        @DisplayName("Should delete content and associated files successfully")
        void testDeleteContent_WithFiles_DeletesFilesAndContent() throws Exception {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(adminUser);
            
            FileModel file1 = new FileModel("file1.txt", "/path1", "txt", "text/plain");
            FileModel file2 = new FileModel("file2.txt", "/path2", "txt", "text/plain");
            
            ContentModel existingContent = new ContentModel();
            existingContent.setFiles(Arrays.asList(file1, file2));
            
            CourseModel course = new CourseModel("course-1", "code-1", "Course 1", null, null, null, null);
            course.addInstructor(adminUser);
            course.addContent(existingContent);
            existingContent.setCourse(course);
            
            when(contentRepository.findById("content-1")).thenReturn(Optional.of(existingContent));

            // Act
            assertDoesNotThrow(() -> {
                contentService.deleteContent("content-1");
            });

            // Assert
            verify(contentRepository, times(1)).deleteById("content-1");
        }
    }


    // ========== Helper Methods ==========
    void setupContentForUser(UserModel user, List<ContentModel> contentList, Role role) {
        if (role == Role.ADMIN) {
            when(contentRepository.findAll()).thenReturn(contentList);
        } else if (role == Role.INSTRUCTOR) {
            for (int i = 0; i < contentList.size(); i++) {
                if (i % 2 == 0) {
                    CourseModel course = new CourseModel("id-"+i, "course-"+i, "Course 1", null, null, null, null);
                    course.addInstructor(user);
                    user.addTaughtCourse(course);
                    course.addContent(contentList.get(i));
                    contentList.get(i).setCourse(course);
                } else {
                    setupContentForUser(user, contentList.get(i));
                }
            }
        } else {
            setupContentForUser(user, contentList);
        }
    }

    void setupContentForUser(UserModel user, ContentModel content) {
        CourseModel course = new CourseModel("4342", "course-1", "Course 1", null, null, null, null);
        user.addEnrollment(new EnrollmentModel("", null, user, course));
        course.addContent(content);
        content.setCourse(course);
    }
    void setupContentForUser(UserModel user, List<ContentModel> contentList) {
        for (ContentModel content : contentList) {
            setupContentForUser(user, content);
        }
    }
}
