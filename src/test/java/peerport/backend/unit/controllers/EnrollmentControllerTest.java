package peerport.backend.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import peerport.backend.controllers.EnrollmentController;
import peerport.backend.model.EnrollmentModel;
import peerport.backend.service.EnrollmentService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentController Unit Tests")
class EnrollmentControllerTest {

    @InjectMocks
    private EnrollmentController enrollmentController;

    @Mock
    private EnrollmentService enrollmentService;

    @Test
    void getAllEnrollments_returns200AndBody() {
        List<EnrollmentModel> enrollments = List.of(new EnrollmentModel());
        when(enrollmentService.getAllEnrollments()).thenReturn(enrollments);

        ResponseEntity<List<EnrollmentModel>> response = enrollmentController.getAllEnrollments();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getEnrollmentById_whenFound_returns200() {
        EnrollmentModel enrollment = new EnrollmentModel();
        when(enrollmentService.getEnrollmentById("e1")).thenReturn(Optional.of(enrollment));

        ResponseEntity<EnrollmentModel> response = enrollmentController.getEnrollmentById("e1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(enrollment, response.getBody());
    }

    @Test
    void getEnrollmentById_whenMissing_returns404() {
        when(enrollmentService.getEnrollmentById("missing")).thenReturn(Optional.empty());

        ResponseEntity<EnrollmentModel> response = enrollmentController.getEnrollmentById("missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void createEnrollment_returns201() {
        EnrollmentModel enrollment = new EnrollmentModel();
        when(enrollmentService.createEnrollment(enrollment)).thenReturn(enrollment);

        ResponseEntity<EnrollmentModel> response = enrollmentController.createEnrollment(enrollment);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(enrollment, response.getBody());
    }

    @Test
    void updateEnrollment_whenFound_returns200() {
        EnrollmentModel enrollment = new EnrollmentModel();
        when(enrollmentService.updateEnrollment("e1", enrollment)).thenReturn(Optional.of(enrollment));

        ResponseEntity<EnrollmentModel> response = enrollmentController.updateEnrollment("e1", enrollment);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(enrollment, response.getBody());
    }

    @Test
    void updateEnrollment_whenMissing_returns404() {
        EnrollmentModel enrollment = new EnrollmentModel();
        when(enrollmentService.updateEnrollment("missing", enrollment)).thenReturn(Optional.empty());

        ResponseEntity<EnrollmentModel> response = enrollmentController.updateEnrollment("missing", enrollment);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteEnrollment_whenDeleted_returns204() {
        when(enrollmentService.deleteEnrollment("e1")).thenReturn(true);

        ResponseEntity<Void> response = enrollmentController.deleteEnrollment("e1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(enrollmentService).deleteEnrollment("e1");
    }

    @Test
    void deleteEnrollment_whenMissing_returns404() {
        when(enrollmentService.deleteEnrollment("missing")).thenReturn(false);

        ResponseEntity<Void> response = enrollmentController.deleteEnrollment("missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
