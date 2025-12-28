package peerport.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.EnrollmentRepository;
import peerport.backend.model.EnrollmentModel;

@Service
public class EnrollmentService {
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // Create Enrollment
    public EnrollmentModel createEnrollment(EnrollmentModel enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    // Get all enrollments
    public List<EnrollmentModel> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    // Get enrollment by ID
    public Optional<EnrollmentModel> getEnrollmentById(String enrollmentId) {
        return enrollmentRepository.findById(enrollmentId);
    }

    // Update enrollment
    public Optional<EnrollmentModel> updateEnrollment(String enrollmentId, EnrollmentModel updatedEnrollment) {
        return enrollmentRepository.findById(enrollmentId).map(enrollment -> {
            enrollment.setDateEnrolled(updatedEnrollment.getDateEnrolled());
            enrollment.setUser(updatedEnrollment.getUser());
            enrollment.setCourse(updatedEnrollment.getCourse());
            return enrollmentRepository.save(enrollment);
        });
    }

    // Delete enrollment
    public boolean deleteEnrollment(String enrollmentId) {
        if (enrollmentRepository.existsById(enrollmentId)) {
            enrollmentRepository.deleteById(enrollmentId);
            return true;
        }
        return false;
    }
}
