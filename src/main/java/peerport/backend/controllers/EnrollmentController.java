package peerport.backend.controllers;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	protected static final Logger logger = LoggerFactory.getLogger(EnrollmentController.class);
    
	@Autowired
	private EnrollmentService enrollmentService;

	@GetMapping
	public ResponseEntity<List<EnrollmentModel>> getAllEnrollments() {
		logger.debug("Retrieving all enrollments");

		List<EnrollmentModel> enrollments = enrollmentService.getAllEnrollments();

		logger.debug("Successfully retrieved {} enrollments", enrollments.size());
		return ResponseEntity.ok(enrollments);
	}

	@GetMapping("/{enrollmentId}")
	public ResponseEntity<EnrollmentModel> getEnrollmentById(@PathVariable String enrollmentId) {
		logger.debug("Attempting to retrieve enrollment with ID: {}", enrollmentId);

		Optional<EnrollmentModel> enrollmentOpt = enrollmentService.getEnrollmentById(enrollmentId);
		if (enrollmentOpt.isPresent()) {
			logger.debug("Successfully retrieved enrollment with ID: {}", enrollmentId);
			return ResponseEntity.ok(enrollmentOpt.get());
		}

		logger.info("Enrollment with ID: {} not found", enrollmentId);
		return ResponseEntity.notFound().build();
	}

	@PostMapping
	public ResponseEntity<EnrollmentModel> createEnrollment(@RequestBody EnrollmentModel enrollment) {
		logger.debug("Creating a new enrollment");

		EnrollmentModel savedEnrollment = enrollmentService.createEnrollment(enrollment);

		logger.debug("Successfully created enrollment with ID: {}", savedEnrollment.getEnrollmentId());
		return ResponseEntity.status(HttpStatus.CREATED).body(savedEnrollment);
	}

	@PutMapping("/{enrollmentId}")
	public ResponseEntity<EnrollmentModel> updateEnrollment(@PathVariable String enrollmentId, @RequestBody EnrollmentModel enrollment) {
		logger.debug("Attempting to update enrollment with ID: {}", enrollmentId);

		Optional<EnrollmentModel> updatedEnrollmentOpt = enrollmentService.updateEnrollment(enrollmentId, enrollment);
		if (updatedEnrollmentOpt.isPresent()) {
			logger.debug("Successfully updated enrollment with ID: {}", enrollmentId);
			return ResponseEntity.ok(updatedEnrollmentOpt.get());
		}

		logger.info("Enrollment with ID: {} not found, cannot update", enrollmentId);
		return ResponseEntity.notFound().build();
	}

	@DeleteMapping("/{enrollmentId}")
	public ResponseEntity<Void> deleteEnrollment(@PathVariable String enrollmentId) {
		logger.debug("Deleting enrollment with ID: {}", enrollmentId);

		boolean deleted = enrollmentService.deleteEnrollment(enrollmentId);
		if (deleted) {
			logger.debug("Successfully deleted enrollment with ID: {}", enrollmentId);
			return ResponseEntity.noContent().build();
		}
		
		logger.info("Enrollment with ID: {} not found, cannot delete", enrollmentId);
		return ResponseEntity.notFound().build();
	}
}
