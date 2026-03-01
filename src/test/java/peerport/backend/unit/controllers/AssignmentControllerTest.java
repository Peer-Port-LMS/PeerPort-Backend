package peerport.backend.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import peerport.backend.controllers.AssignmentController;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.model.AssignmentModel;
import peerport.backend.model.CourseModel;
import peerport.backend.service.AssignmentService;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentController Unit Tests")
class AssignmentControllerTest {

    @InjectMocks
    private AssignmentController controller;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void getAllAssignments_returns200() {
        AssignmentModel assignment = buildAssignment("a1");
        when(assignmentService.getAllAssignments()).thenReturn(List.of(assignment));

        ResponseEntity<?> response = controller.getAllAssignments();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAssignmentById_returns200() {
        AssignmentModel assignment = buildAssignment("a1");
        when(assignmentService.getAssignmentById("a1")).thenReturn(assignment);

        ResponseEntity<?> response = controller.getAssignmentById("a1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createAssignment_returns201() {
        AssignmentModel assignment = buildAssignment("a1");
        when(assignmentService.createAssignment(assignment, "c1")).thenReturn(assignment);

        ResponseEntity<?> response = controller.createAssignment("c1", assignment);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createAssignmentWithFiles_whenJsonInvalid_throwsFailedToParse() throws IOException {
        when(objectMapper.readValue("{bad}", AssignmentModel.class)).thenThrow(new RuntimeException("bad json"));

        assertThrows(FailedToParseFormDataException.class, () ->
            controller.createAssignmentWithFiles(
                "c1",
                "{bad}",
                List.of(new MockMultipartFile("files", "a.txt", "text/plain", "x".getBytes()))
            )
        );
    }

    @Test
    void deleteAssignment_returns204() {
        ResponseEntity<Void> response = controller.deleteAssignment("a1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(assignmentService).deleteAssignment("a1");
    }

    private AssignmentModel buildAssignment(String assignmentId) {
        CourseModel course = new CourseModel("c1", "Course", "C1", true, null, new Date(), new Date());
        return new AssignmentModel(assignmentId, "Assignment", "desc", true, true, new Date(), new Date(), new Date(), course);
    }
}
