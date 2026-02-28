package peerport.backend.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import peerport.backend.database.EnrollmentsRepository;
import peerport.backend.model.EnrollmentModel;
import peerport.backend.service.EnrollmentService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService Unit Tests")
class EnrollmentServiceTest {

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Mock
    private EnrollmentsRepository enrollmentsRepository;

    @Test
    void createEnrollment_savesAndReturnsEnrollment() {
        EnrollmentModel enrollment = new EnrollmentModel();
        enrollment.setUser(new peerport.backend.model.UserModel("u1", "User", "u@test.com", null, null, peerport.backend.model.RoleModel.Role.STUDENT));
        when(enrollmentsRepository.save(enrollment)).thenReturn(enrollment);

        EnrollmentModel result = enrollmentService.createEnrollment(enrollment);

        assertEquals(enrollment, result);
        verify(enrollmentsRepository).save(enrollment);
    }

    @Test
    void getAllEnrollments_returnsRepositoryData() {
        List<EnrollmentModel> enrollments = List.of(new EnrollmentModel(), new EnrollmentModel());
        when(enrollmentsRepository.findAll()).thenReturn(enrollments);

        List<EnrollmentModel> result = enrollmentService.getAllEnrollments();

        assertEquals(2, result.size());
        assertEquals(enrollments, result);
    }

    @Nested
    class GetByIdTests {
        @Test
        void getEnrollmentById_whenFound_returnsEnrollment() {
            EnrollmentModel enrollment = new EnrollmentModel();
            when(enrollmentsRepository.findById("e1")).thenReturn(Optional.of(enrollment));

            Optional<EnrollmentModel> result = enrollmentService.getEnrollmentById("e1");

            assertTrue(result.isPresent());
            assertEquals(enrollment, result.get());
        }

        @Test
        void getEnrollmentById_whenMissing_returnsEmpty() {
            when(enrollmentsRepository.findById("missing")).thenReturn(Optional.empty());

            Optional<EnrollmentModel> result = enrollmentService.getEnrollmentById("missing");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class UpdateTests {
        @Test
        void updateEnrollment_whenFound_savesAndReturnsUpdated() {
            EnrollmentModel existing = new EnrollmentModel();
            EnrollmentModel updates = new EnrollmentModel();

            when(enrollmentsRepository.findById("e1")).thenReturn(Optional.of(existing));
            when(enrollmentsRepository.save(existing)).thenReturn(existing);

            Optional<EnrollmentModel> result = enrollmentService.updateEnrollment("e1", updates);

            assertTrue(result.isPresent());
            verify(enrollmentsRepository).save(existing);
        }

        @Test
        void updateEnrollment_whenMissing_returnsEmpty() {
            when(enrollmentsRepository.findById("missing")).thenReturn(Optional.empty());

            Optional<EnrollmentModel> result = enrollmentService.updateEnrollment("missing", new EnrollmentModel());

            assertTrue(result.isEmpty());
            verify(enrollmentsRepository, never()).save(org.mockito.ArgumentMatchers.any(EnrollmentModel.class));
        }
    }

    @Nested
    class DeleteTests {
        @Test
        void deleteEnrollment_whenExists_deletesAndReturnsTrue() {
            when(enrollmentsRepository.existsById("e1")).thenReturn(true);

            boolean result = enrollmentService.deleteEnrollment("e1");

            assertTrue(result);
            verify(enrollmentsRepository).deleteById("e1");
        }

        @Test
        void deleteEnrollment_whenMissing_returnsFalse() {
            when(enrollmentsRepository.existsById("missing")).thenReturn(false);

            boolean result = enrollmentService.deleteEnrollment("missing");

            assertFalse(result);
            verify(enrollmentsRepository, never()).deleteById("missing");
        }
    }
}
