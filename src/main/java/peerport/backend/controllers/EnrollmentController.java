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

	/**
	 * Retrieves all enrollments.
	 *
	 * @return Response entity containing all enrollments.
	 */
	@GetMapping
	public ResponseEntity<List<EnrollmentModel>> getAllEnrollments() {
		logger.debug("Retrieving all enrollments");

		List<EnrollmentModel> enrollments = enrollmentService.getAllEnrollments();

		logger.debug("Successfully retrieved {} enrollments", enrollments.size());
		return ResponseEntity.ok(enrollments);
	}

	/**
	 * Retrieves an enrollment by ID.
	 *
	 * @param enrollmentId The enrollment ID.
	 * @return Response entity containing the enrollment when found, otherwise 404.
	 */
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

	/**
	 * Creates a new enrollment.
	 *
	 * @param enrollment The enrollment payload to create.
	 * @return Response entity containing the created enrollment.
	 */
	@PostMapping
	public ResponseEntity<EnrollmentModel> createEnrollment(@RequestBody EnrollmentModel enrollment) {
		logger.debug("Creating a new enrollment");

		EnrollmentModel savedEnrollment = enrollmentService.createEnrollment(enrollment);

		logger.debug("Successfully created enrollment with ID: {}", savedEnrollment.getEnrollmentId());
		return ResponseEntity.status(HttpStatus.CREATED).body(savedEnrollment);
	}

	/**
	 * Updates an existing enrollment.
	 *
	 * @param enrollmentId The ID of the enrollment to update.
	 * @param enrollment The enrollment payload containing updated values.
	 * @return Response entity containing the updated enrollment when found, otherwise 404.
	 */
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

	/**
	 * Deletes an enrollment by ID.
	 *
	 * @param enrollmentId The ID of the enrollment to delete.
	 * @return Empty response with 204 when deleted, otherwise 404.
	 */
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
