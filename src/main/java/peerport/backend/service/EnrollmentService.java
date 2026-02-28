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

    // Create Enrollment
    public EnrollmentModel createEnrollment(EnrollmentModel enrollment) {
        logger.debug("Attempting to create enrollment for user with ID: {}", enrollment.getUser().getUserId());
        
        EnrollmentModel savedEnrollment = enrollmentRepository.save(enrollment);
        
        logger.debug("Successfully created enrollment with ID: {}", savedEnrollment.getEnrollmentId());
        return savedEnrollment;
    }

    // Get all enrollments
    public List<EnrollmentModel> getAllEnrollments() {
        logger.debug("Retrieving all enrollments from the database");
        return enrollmentRepository.findAll();
    }

    // Get enrollment by ID
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

    // Update enrollment
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

    // Delete enrollment
    public boolean deleteEnrollment(String enrollmentId) {
        logger.debug("Attempting to delete enrollment with ID: {}", enrollmentId);

        if (enrollmentRepository.existsById(enrollmentId)) {
            enrollmentRepository.deleteById(enrollmentId);
            logger.debug("Successfully deleted enrollment with ID: {}", enrollmentId);
            return true;
        }

        logger.warn("Enrollment with ID: {} not found, cannot delete", enrollmentId);
        return false;
    }
}
