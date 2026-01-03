package peerport.backend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import peerport.backend.dto.AssignmentDTO;
import peerport.backend.dto.AssignmentWithCourseDTO;
import peerport.backend.model.AssignmentModel;
import peerport.backend.service.AssignmentService;

/**
 * Controller for handling assignment-related endpoints
 */
@RestController
@RequestMapping("/assignments")
public class AssignmentController {
    
	@Autowired
	private AssignmentService assignmentService;

	/**
	 * Get all assignments.
	 * 
	 * @return List of AssignmentWithCourseDTO
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 */
	@GetMapping
	public ResponseEntity<List<AssignmentWithCourseDTO>> getAllAssignments() {
		// Get all assignments
		List<AssignmentModel> assignments = assignmentService.getAllAssignments();

		// Convert to DTOs
		List<AssignmentWithCourseDTO> assignmentDTOs = assignments.stream()
				.map(AssignmentModel::toAssignmentWithCourseDTO)
				.toList();
		
		// Return the list of DTOs
		return ResponseEntity.ok(assignmentDTOs);
	}

	/**
	 * Get an assignment by its ID.
	 * 
	 * @param assignmentId The ID of the assignment to retrieve.
	 * @return The assignment with the given ID.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to view this assignment.
	 */
	@GetMapping("/{assignmentId}")
	public ResponseEntity<AssignmentDTO> getAssignmentById(@PathVariable String assignmentId) {
		// Get assignment by ID
		AssignmentModel assignment = assignmentService.getAssignmentById(assignmentId);

		// Convert assignment to DTO and return
		return ResponseEntity.ok(assignment.toDTO());
	}

	/**
	 * Create a new assignment for a course.
	 * 
	 * @param courseId The ID of the course to which the assignment belongs.
	 * @param assignment The assignment data to create.
	 * @return The created assignment.
	 * @throws CourseNotFoundException if the course with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to create an assignment for this course.
	 */
	@PostMapping("/{courseId}")
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<AssignmentDTO> createAssignment(@PathVariable String courseId, @Validated @RequestBody AssignmentModel assignment) {
		// Try to create the assignment
		AssignmentModel savedAssignment = assignmentService.createAssignment(assignment, courseId);

		// Return the created assignment with 201 status
		return ResponseEntity.status(HttpStatus.CREATED).body(savedAssignment.toDTO());
	}

	/**
	 * Update an assignment by its ID.
	 * 
	 * @param assignmentId The ID of the assignment to update.
	 * @param assignment The assignment data to update.
	 * @return The updated assignment.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to update this assignment.
	 */
	@PutMapping("/{assignmentId}")
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<AssignmentDTO> updateAssignment(@PathVariable String assignmentId, @RequestBody AssignmentModel assignment) {
		// Update the assignment
		AssignmentModel updatedAssignment = assignmentService.updateAssignment(assignmentId, assignment);

		// Return the updated assignment as DTO
		return ResponseEntity.ok(updatedAssignment.toDTO());
	}

	/**
	 * Patch an assignment by its ID.
	 * 
	 * @param assignmentId The ID of the assignment to patch.
	 * @param assignment The assignment data to patch.
	 * @return The patched assignment.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to patch this assignment.
	 */
	@PatchMapping("/{assignmentId}")
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<AssignmentDTO> patchAssignment(@PathVariable String assignmentId, @RequestBody AssignmentModel assignment) {
		// Patch the assignment
		AssignmentModel patchedAssignment = assignmentService.patchAssignment(assignmentId, assignment);

		// Return the patched assignment as DTO
		return ResponseEntity.ok(patchedAssignment.toDTO());
	}

	/**
	 * Delete an assignment by its ID.
	 * @param assignmentId The ID of the assignment to delete.
	 * @return A ResponseEntity with no content status if deletion is successful.
	 * @throws AssignmentNotFoundException if the assignment with the given ID does not exist.
	 * @throws UserNotAuthenticatedException if the user is not authenticated to perform this action.
	 * @throws UserNotAuthorizedException if the user is not authorized to delete this assignment.
	 */
	@DeleteMapping("/{assignmentId}")
	@PreAuthorize("@authservice.hasAnyRole(@authservice.ADMIN, @authservice.INSTRUCTOR)")
	public ResponseEntity<Void> deleteAssignment(@PathVariable String assignmentId) {
		// Delete the assignment
		assignmentService.deleteAssignment(assignmentId);
		
		// Return no content status
		return ResponseEntity.noContent().build();
	}
}
