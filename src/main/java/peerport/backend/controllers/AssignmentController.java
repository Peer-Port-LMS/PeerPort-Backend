package peerport.backend.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import peerport.backend.model.AssignmentModel;
import peerport.backend.service.AssignmentService;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {
    
	@Autowired
	private AssignmentService assignmentService;

	@GetMapping
	public ResponseEntity<List<AssignmentDTO>> getAllAssignments() {
		// Get all assignments
		List<AssignmentModel> assignments = assignmentService.getAllAssignments();

		// Convert to DTOs
		List<AssignmentDTO> assignmentDTOs = assignments.stream()
				.map(AssignmentModel::toDTO)
				.toList();
		
		// Return the list of DTOs
		return ResponseEntity.ok(assignmentDTOs);
	}

	@GetMapping("/{assignmentId}")
	public ResponseEntity<AssignmentDTO> getAssignmentById(@PathVariable String assignmentId) {
		// Get assignment by ID
		Optional<AssignmentModel> assignment = assignmentService.getAssignmentById(assignmentId);

		// Check if assignment exsits
		if (assignment.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		// Convert assignment to DTO and return
		return ResponseEntity.ok(assignment.get().toDTO());
	}

	@PostMapping("/{courseId}")
	public ResponseEntity<AssignmentDTO> createAssignment(@PathVariable String courseId, @Validated @RequestBody AssignmentModel assignment) {
		try {
			// Try to create the assignment
			AssignmentModel savedAssignment = assignmentService.createAssignment(assignment, courseId);

			// Return the created assignment with 201 status
			return ResponseEntity.status(HttpStatus.CREATED).body(savedAssignment.toDTO());
		
		// Catch illegal argument exception
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@PutMapping("/{assignmentId}")
	public ResponseEntity<AssignmentDTO> updateAssignment(@PathVariable String assignmentId, @RequestBody AssignmentModel assignment) {
		// Update the assignment
		Optional<AssignmentModel> updatedAssignment = assignmentService.updateAssignment(assignmentId, assignment);

		// Check if the assignment was found and updated
		if (updatedAssignment.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		// Return the updated assignment as DTO
		return ResponseEntity.ok(updatedAssignment.get().toDTO());
	}

	@PatchMapping("/{assignmentId}")
	public ResponseEntity<AssignmentDTO> patchAssignment(@PathVariable String assignmentId, @RequestBody AssignmentModel assignment) {
		try {
			// Patch the assignment
			AssignmentModel patchedAssignment = assignmentService.patchAssignment(assignmentId, assignment);

			// Return the patched assignment as DTO
			return ResponseEntity.ok(patchedAssignment.toDTO());

		// Handle illegal argument exception
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@DeleteMapping("/{assignmentId}")
	public ResponseEntity<Void> deleteAssignment(@PathVariable String assignmentId) {
		boolean deleted = assignmentService.deleteAssignment(assignmentId);
		if (deleted) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}
}
