package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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

import peerport.backend.database.FilesRepository;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.model.CourseModel;
import peerport.backend.model.EnrollmentModel;
import peerport.backend.model.FileModel;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.model.UserModel;
import peerport.backend.service.AuthService;
import peerport.backend.service.FileService;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Unit Tests")
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    @Mock
    private FilesRepository filesRepository;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "fileUploadSizeLimit", 5L);
    }

    @Nested
    class CheckFileSizeTests {
        @Test
        void checkFileSize_whenTooLarge_throws() {
            MockMultipartFile file = new MockMultipartFile("file", "big.txt", "text/plain", new byte[10]);

            assertThrows(FileSizeLimitExceededException.class, () -> fileService.checkFileSize(file));
        }

        @Test
        void checkFileSize_whenWithinLimit_doesNotThrow() {
            MockMultipartFile file = new MockMultipartFile("file", "small.txt", "text/plain", new byte[4]);

            assertDoesNotThrow(() -> fileService.checkFileSize(file));
        }

        @Test
        void checkFileSizes_whenNull_doesNotThrow() {
            assertDoesNotThrow(() -> fileService.checkFileSizes(null));
        }
    }

    @Nested
    class GetFileByIdTests {
        @Test
        void getFileById_adminCanAccessAnyFile() throws Exception {
            UserModel admin = new UserModel("admin", "Admin", "a@test.com", null, null, Role.ADMIN);
            FileModel file = new FileModel("f.txt", "tmp/f.txt", "txt", "text/plain");

            when(filesRepository.findById("f1")).thenReturn(Optional.of(file));
            when(authService.getCurrentUser()).thenReturn(admin);

            FileModel result = fileService.getFileById("f1");

            assertEquals(file, result);
        }

        @Test
        void getFileById_enrolledUserCanAccessCourseFile() throws Exception {
            UserModel student = new UserModel("u1", "Student", "s@test.com", null, null, Role.STUDENT);
            CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
            EnrollmentModel enrollment = new EnrollmentModel("e1", null, student, course);
            student.addEnrollment(enrollment);
            course.addInstructor(student);

            FileModel file = new FileModel("f.txt", "tmp/f.txt", "txt", "text/plain");
            file.setCourse(course);

            when(filesRepository.findById("f1")).thenReturn(Optional.of(file));
            when(authService.getCurrentUser()).thenReturn(student);

            FileModel result = fileService.getFileById("f1");

            assertEquals(file, result);
        }

        @Test
        void getFileById_whenUnauthorized_throwsNotFound() {
            UserModel student = new UserModel("u1", "Student", "s@test.com", null, null, Role.STUDENT);
            CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
            UserModel other = new UserModel("u2", "Other", "o@test.com", null, null, Role.STUDENT);
            EnrollmentModel enrollment = new EnrollmentModel("e2", null, other, course);
            other.addEnrollment(enrollment);

            FileModel file = new FileModel("f.txt", "tmp/f.txt", "txt", "text/plain");
            file.setCourse(course);

            when(filesRepository.findById("f1")).thenReturn(Optional.of(file));
            when(authService.getCurrentUser()).thenReturn(student);

            assertThrows(FileNotFoundException.class, () -> fileService.getFileById("f1"));
        }

        @Test
        void getFileById_whenMissing_throwsNotFound() {
            when(filesRepository.findById("missing")).thenReturn(Optional.empty());

            assertThrows(FileNotFoundException.class, () -> fileService.getFileById("missing"));
            verify(filesRepository).findById("missing");
        }
    }
}
