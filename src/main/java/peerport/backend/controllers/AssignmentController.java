package peerport.backend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import peerport.backend.model.AssignmentModel;
import peerport.backend.service.AssignmentService;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {
    
	@Autowired
	private AssignmentService assignmentService;

	@GetMapping
	public ResponseEntity<List<AssignmentModel>> getAllAssignments() {
		List<AssignmentModel> assignments = assignmentService.getAllAssignments();
		return ResponseEntity.ok(assignments);
	}

	@GetMapping("/{assignmentId}")
	public ResponseEntity<AssignmentModel> getAssignmentById(@PathVariable String assignmentId) {
		return assignmentService.getAssignmentById(assignmentId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<AssignmentModel> createAssignment(@RequestBody AssignmentModel assignment) {
		AssignmentModel savedAssignment = assignmentService.createAssignment(assignment);
		return ResponseEntity.status(HttpStatus.CREATED).body(savedAssignment);
	}

	@PutMapping("/{assignmentId}")
	public ResponseEntity<AssignmentModel> updateAssignment(@PathVariable String assignmentId, @RequestBody AssignmentModel assignment) {
		return assignmentService.updateAssignment(assignmentId, assignment)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
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
