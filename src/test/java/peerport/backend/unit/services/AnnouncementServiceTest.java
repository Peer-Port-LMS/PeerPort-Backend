package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import peerport.backend.database.AnnouncementsRepository;
import peerport.backend.exceptions.announcements.AnnouncementNotFoundException;
import peerport.backend.exceptions.courses.CourseNotFoundException;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.AnnouncementModel;
import peerport.backend.model.FileModel;
import peerport.backend.model.CourseModel;
import peerport.backend.model.EnrollmentModel;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.model.UserModel;
import peerport.backend.service.AnnouncementService;
import peerport.backend.service.AuthService;
import peerport.backend.service.CourseService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Announcement Service Test")
public class AnnouncementServiceTest {
    
    @InjectMocks
    private AnnouncementService announcementService;

    @Mock
    private AnnouncementsRepository announcementsRepository;

    @Mock
    private AuthService authService;

    @Mock
    private CourseService courseService;

    @Mock
    private peerport.backend.service.FileService fileService;

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
        ReflectionTestUtils.setField(announcementService, "fileUploadSizeLimit", 5_000_000L);
        lenient().when(authService.getCurrentUser()).thenReturn(adminUser);
    }


    @Nested
    @DisplayName("getAllAnnouncements Tests")
    class GetAllAnnouncementsTests {
        @ParameterizedTest
        @MethodSource("provideRoleUsers")
        @DisplayName("{0} User should be able to get all announcements")
        void testGetAllAnnouncements_ValidAnnouncements(Role role, UserModel user) {
            // Arrange
            List<AnnouncementModel> announcementList = List.of(
                new AnnouncementModel("Announcement 1", "Content 1", null, null),
                new AnnouncementModel("Announcement 2", "Content 2", null, null)
            );
            setupAnnouncementsForUser(user, announcementList, role);
            when(authService.getCurrentUser()).thenReturn(user);
            when(announcementsRepository.findAll()).thenReturn(announcementList);

            // Act
            List<AnnouncementModel> result = announcementService.getAllAnnouncements();

            // Assert
            assertTrue(result.size() == announcementList.size());
            assertTrue(announcementList.containsAll(result));
        }

        @Test
        @DisplayName("User with no announcements should get empty list")
        void testGetAllAnnouncements_UserWithNoAnnouncements() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(announcementsRepository.findAll()).thenReturn(List.of());

            // Act
            List<AnnouncementModel> result = announcementService.getAllAnnouncements();

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("User with unknown role should be treated as a student")
        void testGetAllAnnouncements_UnknownRoleUser() {
            // Arrange
            UserModel unknownRoleUser = new UserModel();
            List<AnnouncementModel> announcementList = List.of(
                new AnnouncementModel("Announcement 1", "Content 1", null, null)
            );
            setupAnnouncementsForUser(unknownRoleUser, announcementList, Role.STUDENT);
            when(authService.getCurrentUser()).thenReturn(unknownRoleUser);
            when(announcementsRepository.findAll()).thenReturn(announcementList);

            // Act
            List<AnnouncementModel> result = announcementService.getAllAnnouncements();

            // Assert
            assertTrue(result.size() == announcementList.size());
            assertTrue(announcementList.containsAll(result));
        }

        @Test
        @DisplayName("Unauthenticated user should not be able to get announcements")
        void testGetAllAnnouncements_UnauthenticatedUser() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(null);

            // Act & Assert
            assertThrows(UserNotAuthenticatedException.class, ()-> {
                announcementService.getAllAnnouncements();
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
    @DisplayName("getAnnouncementById Tests")
    class GetAnnouncementById {
        @ParameterizedTest
        @MethodSource("provideRoleUsers")
        @DisplayName("{0} User should be able to get announcement by ID")
        void testGetAnnouncementById_ValidAnnouncement(Role role, UserModel user) {
            // Arrange
            AnnouncementModel announcement = new AnnouncementModel("id-1", "Content 1", null, null);
            setupAnnouncementsForUser(user, List.of(announcement), role);
            when(authService.getCurrentUser()).thenReturn(user);
            when(announcementsRepository.findById("id-1")).thenReturn(java.util.Optional.of(announcement));

            // Act
            AnnouncementModel result = announcementService.getAnnouncementById("id-1");

            // Assert
            assertTrue(result != null);
            assertTrue(result.equals(announcement));
        }

        @Test
        @DisplayName("Unknown role should be treated as student")
        void testGetAnnouncementById_UnknownRole() {
            // Arrange
            UserModel unknownRoleUser = new UserModel();
            AnnouncementModel announcement = new AnnouncementModel("id-1", "Content 1", null, null);
            setupAnnouncementsForUser(unknownRoleUser, List.of(announcement), Role.STUDENT);
            when(authService.getCurrentUser()).thenReturn(unknownRoleUser);
            when(announcementsRepository.findById("id-1")).thenReturn(java.util.Optional.of(announcement));

            // Act
            AnnouncementModel result = announcementService.getAnnouncementById("id-1");

            // Assert
            assertTrue(result != null);
            assertTrue(result.equals(announcement));
        }

        @Test
        @DisplayName("Unauthenticated user should throw UserNotAuthenticatedException")
        void testGetAnnouncementById_UnauthenticatedUser() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(null);

            // Act & Assert
            assertThrows(UserNotAuthenticatedException.class, ()-> {
                announcementService.getAnnouncementById("id-1");
            });
        }

        @Test
        @DisplayName("Unauthorized user should throw exception")
        void testGetAnnouncementById_UnauthorizedUser() {
            // Arrange
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(new AnnouncementModel()));
            when(authService.getCurrentUser()).thenReturn(new UserModel());

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, ()-> {
                announcementService.getAnnouncementById("id-1");
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
    @DisplayName("createAnnouncement Tests")
    class createAnnouncementTests {
        @Test
        @DisplayName("Authenticated user should be able to create announcement")
        void testCreateAnnouncement_AuthenticatedUser() {
            // Arrange
            CourseModel course = new CourseModel("id-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel announcement = new AnnouncementModel("Announcement 1", "Content 1", null, null);
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(announcementsRepository.save(announcement)).thenReturn(announcement);
            when(courseService.getCourseById("id-1")).thenReturn(course);

            // Act
            AnnouncementModel result = announcementService.createAnnouncement("id-1", announcement);

            // Assert
            assertTrue(result != null);
            assertTrue(result.equals(announcement));
        }

        @Test
        @DisplayName("Authenticated user should be able to create an announcement with a file")
        void testCreateAnnouncement_WithFile_AuthenticatedUser() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("id-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel announcement = new AnnouncementModel("Announcement 1", "Content 1", null, null);
            AnnouncementModel savedAnnouncement = new AnnouncementModel("Announcement 1", "Content 1", null, null);
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
            FileModel savedFile = new FileModel("test.pdf", "path/test.pdf", "pdf", "application/pdf");
            
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(courseService.getCourseById("id-1")).thenReturn(course);
            when(announcementsRepository.save(announcement)).thenReturn(savedAnnouncement, savedAnnouncement);
            when(fileService.saveAnnouncementFiles(List.of(file), savedAnnouncement, "id-1"))
                .thenReturn(List.of(savedFile));

            // Act
            AnnouncementModel result = announcementService.createAnnouncement("id-1", announcement, List.of(file));

            // Assert
            assertTrue(result != null);
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(savedFile));
            verify(fileService).saveAnnouncementFiles(List.of(file), savedAnnouncement, "id-1");
            verify(announcementsRepository, times(2)).save(announcement);
        }

        @Test
        @DisplayName("Authenticated user should be able to create an announcement with multiple files")
        void testCreateAnnouncement_WithMultipleFiles_AuthenticatedUser() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("id-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel announcement = new AnnouncementModel("Announcement 1", "Content 1", null, null);
            AnnouncementModel savedAnnouncement = new AnnouncementModel("Announcement 1", "Content 1", null, null);
            MockMultipartFile file1 = new MockMultipartFile("file1", "test1.pdf", "application/pdf", new byte[1024]);
            MockMultipartFile file2 = new MockMultipartFile("file2", "test2.jpg", "image/jpeg", new byte[2048]);
            MockMultipartFile file3 = new MockMultipartFile("file3", "test3.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[3072]);
            FileModel savedFile1 = new FileModel("test1.pdf", "path/test1.pdf", "pdf", "application/pdf");
            FileModel savedFile2 = new FileModel("test2.jpg", "path/test2.jpg", "jpg", "image/jpeg");
            FileModel savedFile3 = new FileModel("test3.docx", "path/test3.docx", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            List<MultipartFile> files = List.of(file1, file2, file3);
            List<FileModel> savedFiles = List.of(savedFile1, savedFile2, savedFile3);
            
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(courseService.getCourseById("id-1")).thenReturn(course);
            when(announcementsRepository.save(announcement)).thenReturn(savedAnnouncement, savedAnnouncement);
            when(fileService.saveAnnouncementFiles(files, savedAnnouncement, "id-1"))
                .thenReturn(savedFiles);

            // Act
            AnnouncementModel result = announcementService.createAnnouncement("id-1", announcement, files);

            // Assert
            assertTrue(result != null);
            assertTrue(result.getFiles().size() == 3);
            assertTrue(result.getFiles().containsAll(savedFiles));
            verify(fileService).saveAnnouncementFiles(files, savedAnnouncement, "id-1");
            verify(announcementsRepository, times(2)).save(announcement);
        }

        @Test
        @DisplayName("Authenticated user creating announcement with wrong course ID should throw CourseNotFoundException")
        void testCreateAnnouncement_WrongCourseId() {
            // Arrange
            AnnouncementModel announcement = new AnnouncementModel("Announcement 1", "Content 1", null, null);
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(courseService.getCourseById("wrong-id")).thenThrow(new RuntimeException("Course not found"));

            // Act & Assert
            assertThrows(CourseNotFoundException.class, () -> {
                announcementService.createAnnouncement("wrong-id", announcement);
            });
        }

        @Test
        @DisplayName("Authenticated user trying to upload a file exceeding size limit should throw FileSizeLimitExceededException")
        void testCreateAnnouncement_FileSizeLimitExceeded() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("id-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel announcement = new AnnouncementModel("Announcement 1", "Content 1", null, null);
            MockMultipartFile largeFile = new MockMultipartFile("file", "largefile.pdf", "application/pdf", new byte[10_000_000]);
            when(authService.getCurrentUser()).thenReturn(instructorUser);
            when(courseService.getCourseById("id-1")).thenReturn(course);

            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> {
                announcementService.createAnnouncement("id-1", announcement, List.of(largeFile));
            });
        }

        @Test
        @DisplayName("Unauthenticated user should throw UserNotAuthenticatedException")
        void testCreateAnnouncement_UnauthenticatedUser() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(null);
            AnnouncementModel announcement = new AnnouncementModel("Announcement 1", "Content 1", null, null);

            // Act & Assert
            assertThrows(UserNotAuthenticatedException.class, ()-> {
                announcementService.createAnnouncement("id-1", announcement);
            });
        }

        @Test
        @DisplayName("Unauthorized user should throw UserNotAuthorizedException")
        void testCreateAnnouncement_UnauthorizedUser() {
            // Arrange
            when(authService.getCurrentUser()).thenReturn(new UserModel());
            AnnouncementModel announcement = new AnnouncementModel("Announcement 1", "Content 1", null, null);

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, ()-> {
                announcementService.createAnnouncement("id-1", announcement);
            });
        }
    }

    @Nested
    @DisplayName("updateAnnouncement Tests")
    class UpdateAnnouncementTests {
        @Test
        @DisplayName("Update announcement title and content without files")
        void testUpdateAnnouncement_TitleAndContentOnly() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel();
            existing.setTitle("Old Title");
            existing.setContent("Old Content");
            existing.setCourse(course);
            AnnouncementModel updated = new AnnouncementModel();
            updated.setTitle("New Title");
            updated.setContent("New Content");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.updateAnnouncement("id-1", updated, null, null, false);

            // Assert
            assertTrue(result.getTitle().equals("New Title"));
            assertTrue(result.getContent().equals("New Content"));
            verify(announcementsRepository).save(existing);
            verify(fileService, never()).saveAnnouncementFiles(any(), any(), any());
        }

        @Test
        @DisplayName("Update announcement with adding single file")
        void testUpdateAnnouncement_AddSingleFile() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
            FileModel savedFile = new FileModel("test.pdf", "path/test.pdf", "pdf", "application/pdf");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAnnouncementFiles(List.of(file), existing, "course-1"))
                .thenReturn(List.of(savedFile));
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.updateAnnouncement("id-1", updated, List.of(file), null, false);

            // Assert
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(savedFile));
            verify(fileService).saveAnnouncementFiles(List.of(file), existing, "course-1");
            verify(announcementsRepository).save(existing);
        }

        @Test
        @DisplayName("Update announcement with adding multiple files")
        void testUpdateAnnouncement_AddMultipleFiles() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            MockMultipartFile file1 = new MockMultipartFile("file1", "test1.pdf", "application/pdf", new byte[1024]);
            MockMultipartFile file2 = new MockMultipartFile("file2", "test2.jpg", "image/jpeg", new byte[2048]);
            FileModel savedFile1 = new FileModel("test1.pdf", "path/test1.pdf", "pdf", "application/pdf");
            FileModel savedFile2 = new FileModel("test2.jpg", "path/test2.jpg", "jpg", "image/jpeg");
            List<MultipartFile> files = List.of(file1, file2);
            List<FileModel> savedFiles = List.of(savedFile1, savedFile2);
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAnnouncementFiles(files, existing, "course-1"))
                .thenReturn(savedFiles);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.updateAnnouncement("id-1", updated, files, null, false);

            // Assert
            assertTrue(result.getFiles().size() == 2);
            assertTrue(result.getFiles().containsAll(savedFiles));
            verify(fileService).saveAnnouncementFiles(files, existing, "course-1");
            verify(announcementsRepository).save(existing);
        }

        @Test
        @DisplayName("Update announcement with removing specific files")
        void testUpdateAnnouncement_RemoveSpecificFiles() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            FileModel keepFile = new FileModel("keep.pdf", "path/keep.pdf", "pdf", "application/pdf");
            FileModel removeFile = new FileModel("remove.pdf", "path/remove.pdf", "pdf", "application/pdf");
            ReflectionTestUtils.setField(keepFile, "fileId", "keep-id");
            ReflectionTestUtils.setField(removeFile, "fileId", "remove-id");
            existing.getFiles().addAll(List.of(keepFile, removeFile));
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            doNothing().when(fileService).deleteFile(removeFile);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.updateAnnouncement("id-1", updated, null, List.of("remove-id"), false);

            // Assert
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(keepFile));
            verify(fileService).deleteFile(removeFile);
            verify(announcementsRepository).save(existing);
        }

        @Test
        @DisplayName("Update announcement with replaceAll removes all existing files and adds new ones")
        void testUpdateAnnouncement_ReplaceAllFiles() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            FileModel oldFile1 = new FileModel("old1.pdf", "path/old1.pdf", "pdf", "application/pdf");
            FileModel oldFile2 = new FileModel("old2.pdf", "path/old2.pdf", "pdf", "application/pdf");
            existing.getFiles().addAll(List.of(oldFile1, oldFile2));
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            MockMultipartFile newFile = new MockMultipartFile("file", "new.pdf", "application/pdf", new byte[1024]);
            FileModel savedNewFile = new FileModel("new.pdf", "path/new.pdf", "pdf", "application/pdf");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            doNothing().when(fileService).deleteFile(any(FileModel.class));
            when(fileService.saveAnnouncementFiles(List.of(newFile), existing, "course-1"))
                .thenReturn(List.of(savedNewFile));
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.updateAnnouncement("id-1", updated, List.of(newFile), null, true);

            // Assert
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(savedNewFile));
            verify(fileService, times(2)).deleteFile(any(FileModel.class));
            verify(fileService).saveAnnouncementFiles(List.of(newFile), existing, "course-1");
            verify(announcementsRepository).save(existing);
        }

        @Test
        @DisplayName("Update announcement with files collection initially null")
        void testUpdateAnnouncement_FilesInitiallyNull() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            existing.setFiles(null);
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
            FileModel savedFile = new FileModel("test.pdf", "path/test.pdf", "pdf", "application/pdf");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAnnouncementFiles(List.of(file), existing, "course-1"))
                .thenReturn(List.of(savedFile));
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.updateAnnouncement("id-1", updated, List.of(file), null, false);

            // Assert
            assertTrue(result.getFiles() != null);
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(savedFile));
            verify(fileService).saveAnnouncementFiles(List.of(file), existing, "course-1");
        }

        @Test
        @DisplayName("Update announcement throws exception when file exceeds size limit")
        void testUpdateAnnouncement_FileSizeLimitExceeded() throws Exception {
            // Arrange
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            MockMultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[10_000_000]);

            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> {
                announcementService.updateAnnouncement("id-1", updated, List.of(largeFile), null, false);
            });
            
            verify(announcementsRepository, never()).findById(any());
            verify(announcementsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Update announcement with null files list keeps existing files")
        void testUpdateAnnouncement_NullFilesList() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            FileModel existingFile = new FileModel("existing.pdf", "path/existing.pdf", "pdf", "application/pdf");
            existing.getFiles().add(existingFile);
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.updateAnnouncement("id-1", updated, null, null, false);

            // Assert
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(existingFile));
            verify(fileService, never()).saveAnnouncementFiles(any(), any(), any());
            verify(fileService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("Update announcement with empty removeFileIds list keeps all files")
        void testUpdateAnnouncement_EmptyRemoveFileIdsList() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel();
            existing.setTitle("Title");
            existing.setContent("Content");
            existing.setCourse(course);
            FileModel existingFile = new FileModel("existing.pdf", "path/existing.pdf", "pdf", "application/pdf");
            existing.getFiles().add(existingFile);
            AnnouncementModel updated = new AnnouncementModel();
            updated.setTitle("New Title");
            updated.setContent("New Content");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.updateAnnouncement("id-1", updated, null, List.of(), false);

            // Assert
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(existingFile));
            verify(fileService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("Update announcement with replaceAll=false and null files does not delete existing files")
        void testUpdateAnnouncement_ReplaceAllFalseWithNullFiles() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            FileModel existingFile = new FileModel("existing.pdf", "path/existing.pdf", "pdf", "application/pdf");
            existing.getFiles().add(existingFile);
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.updateAnnouncement("id-1", updated, null, null, false);

            // Assert
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(existingFile));
            verify(fileService, never()).deleteFile(any());
        }
        
        @Test
        @DisplayName("Update announcement throws FileSizeLimitExceededExcetption when one of multiple files exceeds size limit")
        void testUpdateAnnouncement_MultipleFilesOneExceedsSizeLimit() throws Exception {
            // Arrange
            MockMultipartFile file1 = new MockMultipartFile("file1", "test1.pdf", "application/pdf", new byte[1024]);
            MockMultipartFile largeFile = new MockMultipartFile("file2", "large.pdf", "application/pdf", new byte[10_000_000]);
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            List<MultipartFile> files = List.of(file1, largeFile);

            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> {
                announcementService.updateAnnouncement("id-1", updated, files, null, false);
            });
            
            verify(announcementsRepository, never()).findById(any());
            verify(announcementsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Update announcement throws FileSizeLimitExceededException when size of added file exceeds limit")
        void testUpdateAnnouncement_SingleFileExceedsSizeLimit() throws Exception {
            // Arrange
            MockMultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[10_000_000]);
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);

            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> {
                announcementService.updateAnnouncement("id-1", updated, List.of(largeFile), null, false);
            });
            
            verify(announcementsRepository, never()).findById(any());
            verify(announcementsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Update announcement throws AnnouncementNotFoundException when announcement does not exist")
        void testUpdateAnnouncement_AnnouncementNotFound() {
            // Arrange
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AnnouncementNotFoundException.class, () -> {
                announcementService.updateAnnouncement("id-1", updated, null, null, false);
            });
        }

        @Test
        @DisplayName("Update announcement throws UserNotAuthorizedException when user is not authorized")
        void testUpdateAnnouncement_UserNotAuthorized() {
            // Arrange
            UserModel unauthorizedUser = new UserModel();
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            when(authService.getCurrentUser()).thenReturn(unauthorizedUser);

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> {
                announcementService.updateAnnouncement("id-1", updated, null, null, false);
            });
        }

        @Test
        @DisplayName("Update announcement propogates IOException from file service")
        void testUpdateAnnouncement_IOExceptionFromFileService() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            AnnouncementModel updated = new AnnouncementModel("New Title", "New Content", null, null);
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAnnouncementFiles(List.of(file), existing, "course-1"))
                .thenThrow(new IOException("File save error"));

            // Act & Assert
            assertThrows(IOException.class, () -> {
                announcementService.updateAnnouncement("id-1", updated, List.of(file), null, false);
            });
        }
    }

    @Nested
    @DisplayName("pathcAnnouncement Tests")
    class PatchAnnouncementTests {
        @Test
        @DisplayName("Patch announcement title only")
        void testPatchAnnouncement_TitleOnly() {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel();
            existing.setTitle("Old Title");
            existing.setContent("Old Content");
            existing.setCourse(course);
            AnnouncementModel patch = new AnnouncementModel();
            patch.setTitle("New Title");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.patchAnnouncement("id-1", patch);

            // Assert
            assertTrue(result.getTitle().equals("New Title"));
            assertTrue(result.getContent().equals("Old Content"));
            verify(announcementsRepository).save(existing);
        }

        @Test
        @DisplayName("Patch announcement content only")
        void testPatchAnnouncement_ContentOnly() {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel();
            existing.setTitle("Old Title");
            existing.setContent("Old Content");
            existing.setCourse(course);
            AnnouncementModel patch = new AnnouncementModel();
            patch.setContent("New Content");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.patchAnnouncement("id-1", patch);

            // Assert
            assertTrue(result.getTitle().equals("Old Title"));
            assertTrue(result.getContent().equals("New Content"));
            verify(announcementsRepository).save(existing);
        }

        @Test
        @DisplayName("Patch announcement title and content")
        void testPatchAnnouncement_TitleAndContent() {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Old Title", "Old Content", null, null);
            existing.setCourse(course);
            AnnouncementModel patch = new AnnouncementModel();
            patch.setTitle("New Title");
            patch.setContent("New Content");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.patchAnnouncement("id-1", patch);

            // Assert
            assertTrue(result.getTitle().equals("New Title"));
            assertTrue(result.getContent().equals("New Content"));
            verify(announcementsRepository).save(existing);
        }

        @Test
        @DisplayName("Patch announcement with files adds single file")
        void testPatchAnnouncement_AddSingleFile() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            AnnouncementModel patch = new AnnouncementModel();
            patch.setTitle("New Title");
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
            FileModel savedFile = new FileModel("test.pdf", "path/test.pdf", "pdf", "application/pdf");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAnnouncementFiles(List.of(file), existing, "course-1"))
                .thenReturn(List.of(savedFile));
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.patchAnnouncement("id-1", patch, List.of(file), null, false);

            // Assert
            assertTrue(result.getTitle().equals("New Title"));
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(savedFile));
            verify(fileService).saveAnnouncementFiles(List.of(file), existing, "course-1");
            verify(announcementsRepository, times(2)).save(existing);
        }

        @Test
        @DisplayName("Patch announcement with files removes specific files")
        void testPatchAnnouncement_RemoveSpecificFiles() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            FileModel keepFile = new FileModel("keep.pdf", "path/keep.pdf", "pdf", "application/pdf");
            FileModel removeFile = new FileModel("remove.pdf", "path/remove.pdf", "pdf", "application/pdf");
            ReflectionTestUtils.setField(keepFile, "fileId", "keep-id");
            ReflectionTestUtils.setField(removeFile, "fileId", "remove-id");
            existing.getFiles().addAll(List.of(keepFile, removeFile));
            AnnouncementModel patch = new AnnouncementModel();
            patch.setContent("New Content");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            doNothing().when(fileService).deleteFile(removeFile);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.patchAnnouncement("id-1", patch, null, List.of("remove-id"), false);

            // Assert
            assertTrue(result.getContent().equals("New Content"));
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(keepFile));
            verify(fileService).deleteFile(removeFile);
            verify(announcementsRepository, times(2)).save(existing);
        }

        @Test
        @DisplayName("Patch announcement with replaceAll removes all existing files and adds new ones")
        void testPatchAnnouncement_ReplaceAllFiles() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            FileModel oldFile1 = new FileModel("old1.pdf", "path/old1.pdf", "pdf", "application/pdf");
            FileModel oldFile2 = new FileModel("old2.pdf", "path/old2.pdf", "pdf", "application/pdf");
            existing.getFiles().addAll(List.of(oldFile1, oldFile2));
            AnnouncementModel patch = new AnnouncementModel();
            MockMultipartFile newFile = new MockMultipartFile("file", "new.pdf", "application/pdf", new byte[1024]);
            FileModel savedNewFile = new FileModel("new.pdf", "path/new.pdf", "pdf", "application/pdf");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            doNothing().when(fileService).deleteFile(any(FileModel.class));
            when(fileService.saveAnnouncementFiles(List.of(newFile), existing, "course-1"))
                .thenReturn(List.of(savedNewFile));
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.patchAnnouncement("id-1", patch, List.of(newFile), null, true);

            // Assert
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(savedNewFile));
            verify(fileService, times(2)).deleteFile(any(FileModel.class));
            verify(fileService).saveAnnouncementFiles(List.of(newFile), existing, "course-1");
            verify(announcementsRepository, times(2)).save(existing);
        }

        @Test
        @DisplayName("Patch announcement with files initializes null files collection")
        void testPatchAnnouncement_FilesInitiallyNull() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            existing.setFiles(null);
            AnnouncementModel patch = new AnnouncementModel();
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
            FileModel savedFile = new FileModel("test.pdf", "path/test.pdf", "pdf", "application/pdf");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAnnouncementFiles(List.of(file), existing, "course-1"))
                .thenReturn(List.of(savedFile));
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.patchAnnouncement("id-1", patch, List.of(file), null, false);

            // Assert
            assertTrue(result.getFiles() != null);
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(savedFile));
            verify(fileService).saveAnnouncementFiles(List.of(file), existing, "course-1");
        }

        @Test
        @DisplayName("Patch announcement throws FileSizeLimitExceededException when file exceeds size limit")
        void testPatchAnnouncement_FileSizeLimitExceeded() throws Exception {
            // Arrange
            AnnouncementModel patch = new AnnouncementModel();
            patch.setTitle("New Title");
            MockMultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[10_000_000]);

            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> {
                announcementService.patchAnnouncement("id-1", patch, List.of(largeFile), null, false);
            });
            
            verify(announcementsRepository, never()).findById(any());
            verify(announcementsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Patch announcement with multiple files where one exceeds size limit throws exception")
        void testPatchAnnouncement_MultipleFilesOneExceedsLimit() throws Exception {
            // Arrange
            AnnouncementModel patch = new AnnouncementModel();
            MockMultipartFile file1 = new MockMultipartFile("file1", "test1.pdf", "application/pdf", new byte[1024]);
            MockMultipartFile largeFile = new MockMultipartFile("file2", "large.pdf", "application/pdf", new byte[10_000_000]);
            List<MultipartFile> files = List.of(file1, largeFile);

            // Act & Assert
            assertThrows(FileSizeLimitExceededException.class, () -> {
                announcementService.patchAnnouncement("id-1", patch, files, null, false);
            });
            
            verify(announcementsRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Patch announcement throws AnnouncementNotFoundException when announcement does not exist")
        void testPatchAnnouncement_AnnouncementNotFound() {
            // Arrange
            AnnouncementModel patch = new AnnouncementModel();
            patch.setTitle("New Title");
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AnnouncementNotFoundException.class, () -> {
                announcementService.patchAnnouncement("id-1", patch);
            });
        }

        @Test
        @DisplayName("Patch announcement throws UserNotAuthorizedException when user is not authorized")
        void testPatchAnnouncement_UserNotAuthorized() {
            // Arrange
            UserModel unauthorizedUser = new UserModel();
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            AnnouncementModel patch = new AnnouncementModel();
            patch.setTitle("New Title");
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            when(authService.getCurrentUser()).thenReturn(unauthorizedUser);

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> {
                announcementService.patchAnnouncement("id-1", patch);
            });
            
            verify(announcementsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Patch announcement propagates IOException from file service")
        void testPatchAnnouncement_IOExceptionFromFileService() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            AnnouncementModel patch = new AnnouncementModel();
            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(fileService.saveAnnouncementFiles(List.of(file), existing, "course-1"))
                .thenThrow(new IOException("File save error"));

            // Act & Assert
            assertThrows(IOException.class, () -> {
                announcementService.patchAnnouncement("id-1", patch, List.of(file), null, false);
            });
        }

        @Test
        @DisplayName("Patch announcement with no file operations keeps existing files")
        void testPatchAnnouncement_NoFileOperationsKeepsExistingFiles() throws Exception {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
            FileModel existingFile = new FileModel("existing.pdf", "path/existing.pdf", "pdf", "application/pdf");
            existing.getFiles().add(existingFile);
            AnnouncementModel patch = new AnnouncementModel();
            patch.setTitle("New Title");
            
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            doNothing().when(courseService).userAllowedToAccessCourse(course);
            doNothing().when(courseService).userAllowedToEditCourse(course);
            when(announcementsRepository.save(existing)).thenReturn(existing);

            // Act
            AnnouncementModel result = announcementService.patchAnnouncement("id-1", patch, null, null, false);

            // Assert
            assertTrue(result.getTitle().equals("New Title"));
            assertTrue(result.getFiles().size() == 1);
            assertTrue(result.getFiles().contains(existingFile));
            verify(fileService, never()).saveAnnouncementFiles(any(), any(), any());
            verify(fileService, never()).deleteFile(any());
            verify(announcementsRepository, times(2)).save(existing);
        }

        
    }

    @Nested
    @DisplayName("deleteAnnouncement Tests")
    class DeleteAnnouncementTests {
        @Test
        @DisplayName("Delete announcement successfully")
        void testDeleteAnnouncement_Success() {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            adminUser.addEnrollment(new EnrollmentModel(null, null, adminUser, course));
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
        
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            when(authService.getCurrentUser()).thenReturn(adminUser);

            // Act
            announcementService.deleteAnnouncement("id-1");

            // Assert
            verify(announcementsRepository).deleteById(existing.getAnnouncementId());
        }

        @Test
        @DisplayName("Delete announcement that does not exist throws AnnouncementNotFoundException")
        void testDeleteAnnouncement_AnnouncementNotFound() {
            // Arrange
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AnnouncementNotFoundException.class, () -> {
                announcementService.deleteAnnouncement("id-1");
            });
        }

        @Test
        @DisplayName("Delete announcement with unauthenticated user throws UserNotAuthenticatedException")
        void testDeleteAnnouncement_UnauthenticatedUserThrowsException() {
            // Arrange
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
        
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            when(authService.getCurrentUser()).thenReturn(null);

            // Act & Assert
            assertThrows(UserNotAuthenticatedException.class, () -> {
                announcementService.deleteAnnouncement("id-1");
            });
        }

        @Test
        @DisplayName("Delete announcement with unauthorized user throws UserNotAuthorizedException")
        void testDeleteAnnouncement_UnauthorizedUserThrowsException() {
            // Arrange
            UserModel regularUser = new UserModel();
            CourseModel course = new CourseModel("course-1", "Course 1", "code", null, "desc", null, null);
            AnnouncementModel existing = new AnnouncementModel("Title", "Content", null, null);
            existing.setCourse(course);
        
            when(announcementsRepository.findById("id-1")).thenReturn(Optional.of(existing));
            when(authService.getCurrentUser()).thenReturn(regularUser);

            // Act & Assert
            assertThrows(UserNotAuthorizedException.class, () -> {
                announcementService.deleteAnnouncement("id-1");
            });
        }

    }

    void setupAnnouncementsForUser(UserModel user, List<AnnouncementModel> announcements, Role role) {
        for (AnnouncementModel announcement : announcements) {
            // Create a course and associate it with the announcement
            CourseModel course = new CourseModel();
            announcement.setCourse(course);
            course.addAnnouncement(announcement);
            user.addEnrollment(new EnrollmentModel("", null, user, course));
        }
    }
}
