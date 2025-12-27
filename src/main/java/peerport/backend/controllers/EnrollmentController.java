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

import peerport.backend.model.EnrollmentModel;
import peerport.backend.service.EnrollmentService;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {
    
	@Autowired
	private EnrollmentService enrollmentService;

	@GetMapping
	public ResponseEntity<List<EnrollmentModel>> getAllEnrollments() {
		List<EnrollmentModel> enrollments = enrollmentService.getAllEnrollments();
		return ResponseEntity.ok(enrollments);
	}

	@GetMapping("/{enrollmentId}")
	public ResponseEntity<EnrollmentModel> getEnrollmentById(@PathVariable String enrollmentId) {
		return enrollmentService.getEnrollmentById(enrollmentId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<EnrollmentModel> createEnrollment(@RequestBody EnrollmentModel enrollment) {
		EnrollmentModel savedEnrollment = enrollmentService.createEnrollment(enrollment);
		return ResponseEntity.status(HttpStatus.CREATED).body(savedEnrollment);
	}

	@PutMapping("/{enrollmentId}")
	public ResponseEntity<EnrollmentModel> updateEnrollment(@PathVariable String enrollmentId, @RequestBody EnrollmentModel enrollment) {
		return enrollmentService.updateEnrollment(enrollmentId, enrollment)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{enrollmentId}")
	public ResponseEntity<Void> deleteEnrollment(@PathVariable String enrollmentId) {
		boolean deleted = enrollmentService.deleteEnrollment(enrollmentId);
		if (deleted) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}
}
