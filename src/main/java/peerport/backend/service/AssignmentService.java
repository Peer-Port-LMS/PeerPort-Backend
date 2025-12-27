package peerport.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.AssignmentRepository;
import peerport.backend.model.AssignmentModel;

@Service
public class AssignmentService {
    
    @Autowired
    private AssignmentRepository assignmentRepository;

    // Create Assignment
    public AssignmentModel createAssignment(AssignmentModel assignment) {
        return assignmentRepository.save(assignment);
    }

    // Get all assignments
    public List<AssignmentModel> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    // Get assignment by ID
    public Optional<AssignmentModel> getAssignmentById(String assignmentId) {
        return assignmentRepository.findById(assignmentId);
    }

    // Update assignment
    public Optional<AssignmentModel> updateAssignment(String assignmentId, AssignmentModel updatedAssignment) {
        return assignmentRepository.findById(assignmentId).map(assignment -> {
            assignment.setName(updatedAssignment.getName());
            assignment.setDescription(updatedAssignment.getDescription());
            assignment.setVisible(updatedAssignment.getVisible());
            assignment.setDueDate(updatedAssignment.getDueDate());
            assignment.setDateCreated(updatedAssignment.getDateCreated());
            assignment.setDateUpdated(updatedAssignment.getDateUpdated());
            assignment.setCourse(updatedAssignment.getCourse());
            return assignmentRepository.save(assignment);
        });
    }

    // Delete assignment
    public boolean deleteAssignment(String assignmentId) {
        if (assignmentRepository.existsById(assignmentId)) {
            assignmentRepository.deleteById(assignmentId);
            return true;
        }
        return false;
    }
}
