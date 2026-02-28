package peerport.backend.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import peerport.backend.controllers.AssignmentSubmissionController;
import peerport.backend.model.AssignmentModel;
import peerport.backend.model.AssignmentSubmissionModel;
import peerport.backend.model.CourseModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.service.AssignmentSubmissionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentSubmissionController Unit Tests")
class AssignmentSubmissionControllerTest {

    @InjectMocks
    private AssignmentSubmissionController controller;

    @Mock
    private AssignmentSubmissionService assignmentSubmissionService;

    @Test
    void getAllAssignmentSubmissions_returns200AndDtos() {
        AssignmentSubmissionModel submission = new AssignmentSubmissionModel();
        submission.setDateSubmitted(new Date());
        when(assignmentSubmissionService.getAllAssignmentSubmissions()).thenReturn(List.of(submission));

        ResponseEntity<?> response = controller.getAllAssignmentSubmissions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAssignmentSubmissionById_returns200WithDetailsDto() {
        AssignmentSubmissionModel submission = buildDetailedSubmission("s1", "u1", "a1", "c1");
        when(assignmentSubmissionService.getSubmissionById("s1")).thenReturn(submission);

        ResponseEntity<?> response = controller.getAssignmentSubmissionById("s1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createSubmission_returns201() {
        AssignmentSubmissionModel request = new AssignmentSubmissionModel();
        AssignmentSubmissionModel saved = new AssignmentSubmissionModel("s1", new Date(), "ok", null, null, List.of());
        when(assignmentSubmissionService.createAssignmentSubmission(request, "a1")).thenReturn(saved);

        ResponseEntity<?> response = controller.createSubmission("a1", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createSubmissionMultipart_returns201() throws IOException {
        AssignmentSubmissionModel request = new AssignmentSubmissionModel();
        AssignmentSubmissionModel saved = new AssignmentSubmissionModel("s1", new Date(), "ok", null, null, List.of());
        MockMultipartFile file = new MockMultipartFile("files", "a.txt", "text/plain", "hello".getBytes());

        when(assignmentSubmissionService.createAssignmentSubmission(request, "a1", List.of(file))).thenReturn(saved);

        ResponseEntity<?> response = controller.createSubmission("a1", request, List.of(file));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void deleteAssignmentSubmission_returns204() {
        ResponseEntity<Void> response = controller.deleteAssignmentSubmission("s1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(assignmentSubmissionService).deleteAssignmentSubmissionById("s1");
    }

    private AssignmentSubmissionModel buildDetailedSubmission(String submissionId, String userId, String assignmentId, String courseId) {
        UserModel user = new UserModel(userId, "Student", "student@test.com", null, null, Role.STUDENT);
        CourseModel course = new CourseModel(courseId, "Course", "C1", true, "desc", new Date(), new Date());
        AssignmentModel assignment = new AssignmentModel(assignmentId, "Assignment", "desc", true, new Date(), new Date(), new Date(), course);
        return new AssignmentSubmissionModel(submissionId, new Date(), "comment", user, assignment, List.of());
    }
}
