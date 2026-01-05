package peerport.backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import peerport.backend.dto.assignments.AssignmentSubmissionDTO;
import peerport.backend.dto.assignments.AssignmentSubmissionWithDetailsDTO;
import peerport.backend.model.AssignmentSubmissionModel;
import peerport.backend.service.AssignmentSubmissionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;




@RestController
@RequestMapping("/submissions/assignments")
public class AssignmentSubmissionController {

    @Autowired
    private AssignmentSubmissionService assignmentSubmissionService;


    /**
     * Get all assignment submissions
     * 
     * @return ResponseEntity containing a list of all assignment submission DTOs
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @GetMapping
    public ResponseEntity<List<AssignmentSubmissionDTO>> getAllAssignmentSubmissions() {
        // Get all the assignment submissions
        List<AssignmentSubmissionModel> submissions = assignmentSubmissionService.getAllAssignmentSubmissions(); 

        // Convert to DTOs
        List<AssignmentSubmissionDTO> submissionDTOs = new ArrayList<>();
        for (AssignmentSubmissionModel model : submissions) {
            submissionDTOs.add(model.toDTO());
        }

        // Return the list of submissions
        return ResponseEntity.ok(submissionDTOs);
    }
    
    /**
     * Get assignment submission by ID
     * 
     * @param assignmentSubmissionId ID of the assignment submission
     * @return ResponseEntity containing the assignment submission with details DTO
     * @throws AssignmentSubmissionNotFoundException If the assignment submission is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to view the submission (Handled in GlobalExceptionHandler)
     */
    @GetMapping("/{assignmentSubmissionId}")
    public ResponseEntity<AssignmentSubmissionWithDetailsDTO> getAssignmentSubmissionById(@PathVariable String assignmentSubmissionId) {
        // Get the assignment submission by ID
        AssignmentSubmissionModel submission = assignmentSubmissionService.getSubmissionById(assignmentSubmissionId);

        // Convert to a DTO and return
        return ResponseEntity.ok(submission.toWithDetailsDTO());
    }

    /**
     * Create a new assignment submission
     * 
     * @param assignmentId ID of the assignment
     * @param submissionData Data for the new assignment submission
     * @return ResponseEntity containing the created assignment submission DTO
     * @throws AssignmentNotFoundException If the assignment is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to submit to the assignment (Handled in GlobalExceptionHandler)
     */
    @PostMapping("/{assignmentId}")
    public ResponseEntity<AssignmentSubmissionDTO> createSubmission(@PathVariable String assignmentId, @RequestBody AssignmentSubmissionModel submissionData) {
        // Create the assignment submission
        AssignmentSubmissionModel submission = assignmentSubmissionService.createAssignmentSubmission(submissionData, assignmentId);

        // Convert to DTO and return
        return ResponseEntity.status(HttpStatus.CREATED).body(submission.toDTO());
    }

    /**
     * Create a new assignment submission with files
     * 
     * @param assignmentId ID of the assignment
     * @param submissionData Data for the new assignment submission
     * @param files Files to upload with the submission
     * @return ResponseEntity containing the created assignment submission DTO
     * @throws AssignmentNotFoundException If the assignment is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to submit to the assignment (Handled in GlobalExceptionHandler)
     * @throws FileSizeLimitExceededException If any file exceeds the size limit (Handled in GlobalExceptionHandler)
     * @throws IOException If there is an error uploading the files
     */
    @PostMapping(value="/{assignmentId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssignmentSubmissionDTO> createSubmission(
        @PathVariable String assignmentId, 
        @RequestPart(value="submission", required=true) AssignmentSubmissionModel submissionData,
        @RequestPart(value="files", required=false) List<MultipartFile> files
    ) throws IOException {
        // Create the assignment submission
        AssignmentSubmissionModel submission = assignmentSubmissionService.createAssignmentSubmission(submissionData, assignmentId, files);

        // Convert to DTO and return
        return ResponseEntity.status(HttpStatus.CREATED).body(submission.toDTO());
    }

    /**
     * Delete an assignment submission by ID
     * 
     * @param assignmentSubmissionId ID of the assignment submission to delete
     * @return ResponseEntity with no content
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws AssignmentSubmissionNotFoundException If the assignment submission is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to delete the submission (Handled in GlobalExceptionHandler)
     */
    @DeleteMapping("/{assignmentSubmissionId}")
    public ResponseEntity<Void> deleteAssignmentSubmission(@PathVariable String assignmentSubmissionId) {
        // Delete the assignment submission
        assignmentSubmissionService.deleteAssignmentSubmissionById(assignmentSubmissionId);

        // Return no content
        return ResponseEntity.noContent().build();
    }
}
