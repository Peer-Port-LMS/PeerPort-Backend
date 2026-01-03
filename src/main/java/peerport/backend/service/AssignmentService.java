package peerport.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.AssignmentsRepository;
import peerport.backend.exceptions.assignments.AssignmentNotFoundException;
import peerport.backend.model.AssignmentModel;
import peerport.backend.model.CourseModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;

@Service
public class AssignmentService {
    
    @Autowired
    private AuthService authService;

    @Autowired
    private AssignmentsRepository assignmentRepository;

    @Autowired
    private CourseService courseService;

    // Create Assignment
    public AssignmentModel createAssignment(AssignmentModel assignment, String courseId) {
        // Get the course by ID
        CourseModel course = courseService.getCourseById(courseId);
        
        // Set the course to the assignment
        assignment.setCourse(course);

        // Return saved assignment
        return assignmentRepository.save(assignment);
    }

    // Get all assignments
    public List<AssignmentModel> getAllAssignments() throws IllegalArgumentException {
        // Get the users role
        UserModel user = authService.getCurrentUser();

        // Check user role
        if (user.getRole() == Role.ADMIN) {
            return assignmentRepository.findAll();
        }

        // Get all courses the user is in
        CourseModel[] courses = courseService.getAllCourses().toArray(new CourseModel[0]);

        // Get all the assignments for those courses
        List<AssignmentModel> assignments = new ArrayList<>();
        for (CourseModel course: courses) {
            assignments.addAll(course.getAssignments());
        }

        // Return the assignments
        return assignments;
    }

    // Get assignment by ID
    public AssignmentModel getAssignmentById(String assignmentId) {
        // Get the assignment by ID
        Optional<AssignmentModel> assignment = assignmentRepository.findById(assignmentId);

        // Check if the assignment exists or not
        if (assignment.isEmpty()) {
            throw new AssignmentNotFoundException(assignmentId);
        }
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

    // Patch / partially update assignment
    public AssignmentModel patchAssignment(String assignmentId, AssignmentModel updatedFields) throws IllegalArgumentException {
        // Get the existing assignment
        Optional<AssignmentModel> assignment = assignmentRepository.findById(assignmentId);

        // If assignment doesn't exist return empty
        if (assignment.isEmpty()) {
            throw new IllegalArgumentException("Assignment with ID " + assignmentId + " not found");
        }

        // If assignment exists, update only the provided fields
        AssignmentModel existingAssignment = assignment.get();
        if (updatedFields.getName() != null) {
            existingAssignment.setName(updatedFields.getName());
        }
        if (updatedFields.getDescription() != null) {
            existingAssignment.setDescription(updatedFields.getDescription());
        }
        if (updatedFields.getVisible() != null) {
            existingAssignment.setVisible(updatedFields.getVisible());
        }
        if (updatedFields.getDueDate() != null) {
            existingAssignment.setDueDate(updatedFields.getDueDate());
        }
        if (updatedFields.getDateUpdated() != null) {
            existingAssignment.setDateUpdated(updatedFields.getDateUpdated());
        }
        if (updatedFields.getCourse() != null) {
            existingAssignment.setCourse(updatedFields.getCourse());
        }

        // Save and return the updated assignment
        return assignmentRepository.save(existingAssignment);
    }

    // Delete assignment
    public void deleteAssignment(String assignmentId) {
        // Check if the user is authorized to delete the assignment

        // Delete the assignment by ID
        assignmentRepository.deleteById(assignmentId);
    }


    // Helpers //
    private void userAllowedToEditAssignment(AssignmentModel assignment) {
        // Check if user is allowed to edit the assignment
        courseService.userAllowedToEditCourse(assignment.getCourse());
    }

    private void userAllowedToEditAssignment(String assignmentId) {
        // Get the assignment by ID
        AssignmentModel assignment = getAssignmentById(assignmentId);

        // Check if user is allowed to edit the assignment
        userAllowedToEditAssignment(assignment);
    }
}
