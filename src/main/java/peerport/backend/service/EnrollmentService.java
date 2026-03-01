package peerport.backend.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.EnrollmentsRepository;
import peerport.backend.model.EnrollmentModel;

@Service
public class EnrollmentService {
    protected static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);
    
    @Autowired
    private EnrollmentsRepository enrollmentRepository;

    /**
     * Creates a new enrollment.
     *
     * @param enrollment The enrollment payload to create.
     * @return The saved enrollment.
     */
    public EnrollmentModel createEnrollment(EnrollmentModel enrollment) {
        logger.debug("Attempting to create enrollment for user with ID: {}", enrollment.getUser().getUserId());
        
        EnrollmentModel savedEnrollment = enrollmentRepository.save(enrollment);
        
        logger.debug("Successfully created enrollment with ID: {}", savedEnrollment.getEnrollmentId());
        return savedEnrollment;
    }

    /**
     * Retrieves all enrollments.
     *
     * @return A list of all enrollments.
     */
    public List<EnrollmentModel> getAllEnrollments() {
        logger.debug("Retrieving all enrollments from the database");
        return enrollmentRepository.findAll();
    }

    /**
     * Retrieves an enrollment by ID.
     *
     * @param enrollmentId The enrollment ID.
     * @return An optional containing the enrollment when found, otherwise empty.
     */
    public Optional<EnrollmentModel> getEnrollmentById(String enrollmentId) {
        logger.debug("Attempting to retrieve enrollment with ID: {}", enrollmentId);

        Optional<EnrollmentModel> enrollmentOpt = enrollmentRepository.findById(enrollmentId);

        if (enrollmentOpt.isPresent()) {
            logger.debug("Successfully retrieved enrollment with ID: {}", enrollmentId);
        } else {
            logger.warn("Enrollment with ID: {} not found", enrollmentId);
        }
        return enrollmentOpt;
    }

    /**
     * Updates an existing enrollment.
     *
     * @param enrollmentId The enrollment ID to update.
     * @param updatedEnrollment The enrollment payload containing updated values.
     * @return An optional containing the updated enrollment when found, otherwise empty.
     */
    public Optional<EnrollmentModel> updateEnrollment(String enrollmentId, EnrollmentModel updatedEnrollment) {
        logger.debug("Attempting to update enrollment with ID: {}", enrollmentId);
        return enrollmentRepository.findById(enrollmentId).map(enrollment -> {
            enrollment.setDateEnrolled(updatedEnrollment.getDateEnrolled());
            enrollment.setUser(updatedEnrollment.getUser());
            enrollment.setCourse(updatedEnrollment.getCourse());

            logger.debug("Successfully updated enrollment with ID: {}", enrollmentId);
            return enrollmentRepository.save(enrollment);
        });
    }

    /**
     * Deletes an enrollment by ID.
     *
     * @param enrollmentId The enrollment ID to delete.
     * @return True when deletion succeeds, otherwise false.
     */
    public boolean deleteEnrollment(String enrollmentId) {
        logger.debug("Attempting to delete enrollment with ID: {}", enrollmentId);

        // Check if the enrollment exists before attempting to delete
        if (enrollmentRepository.existsById(enrollmentId)) {
            enrollmentRepository.deleteById(enrollmentId);
            logger.debug("Successfully deleted enrollment with ID: {}", enrollmentId);
            return true;
        }

        logger.warn("Enrollment with ID: {} not found, cannot delete", enrollmentId);
        return false;
    }
}
