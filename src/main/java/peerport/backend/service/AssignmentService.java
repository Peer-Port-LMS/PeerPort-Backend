package peerport.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import peerport.backend.database.AssignmentsRepository;
import peerport.backend.exceptions.assignments.AssignmentNotFoundException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
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

    /**
     * Create assignment
     * 
     * @param assignment - AssignmentModel to create
     * @param courseId - ID of the course to create the assignment for
     * @return The created AssignmentModel
     * @throws CourseNotFoundException if course not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AssignmentModel createAssignment(AssignmentModel assignment, String courseId) {
        // Get the course by ID
        CourseModel course = courseService.getCourseById(courseId);

        // Check if the user is allowed to edit the course
        courseService.userAllowedToEditCourse(course);
        
        // Set the course to the assignment
        assignment.setCourse(course);

        // Return saved assignment
        return assignmentRepository.save(assignment);
    }

    /**
     * Get all assignments
     * 
     * @return List of AssignmentModels
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to view (Handled in GlobalExceptionHandler)
     */
    public List<AssignmentModel> getAllAssignments() {
        // Get the users role
        UserModel user = authService.getCurrentUser();

        // Check user role
        if (user.getRole() == Role.ADMIN) {
            return assignmentRepository.findAll();
        }

        // Get all courses the user is in
        List<CourseModel> courses = courseService.getAllCourses();

        // Get all the assignments for those courses
        List<AssignmentModel> assignments = new ArrayList<>();
        for (CourseModel course: courses) {
            assignments.addAll(course.getAssignments());
        }

        // Return the assignments
        return assignments;
    }

    /**
     * Get assignment by ID
     * 
     * @param assignmentId - ID of the assignment to get
     * @return AssignmentModel with the given ID
     * @throws AssignmentNotFoundException if assignment not found (Handled in GlobalExceptionHandler)
     */
    public AssignmentModel getAssignmentById(String assignmentId) {
        // Get the assignment by ID
        Optional<AssignmentModel> assignment = assignmentRepository.findById(assignmentId);

        // Check if the assignment exists or not
        if (assignment.isEmpty()) {
            throw new AssignmentNotFoundException(assignmentId);
        }

        // Return the assignment
        return assignment.get();
    }

    /**
     * Update assignment
     * 
     * @param assignmentId - ID of the assignment to update
     * @param updatedAssignment - AssignmentModel containing updated fields
     * @return Updated AssignmentModel
     * @throws AssignmentNotFoundException if assignment not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AssignmentModel updateAssignment(String assignmentId, AssignmentModel updatedAssignment) {
        // Get the assignment
        AssignmentModel assignment = getAssignmentById(assignmentId);

        // Check if the user is authorized to edit the assignment
        userAllowedToEditAssignment(assignment);

        // Update the fields
        assignment.setName(updatedAssignment.getName());
        assignment.setDescription(updatedAssignment.getDescription());
        assignment.setVisible(updatedAssignment.getVisible());
        assignment.setDueDate(updatedAssignment.getDueDate());
        assignment.setDateUpdated(updatedAssignment.getDateUpdated());

        // Return the assignment
        return assignmentRepository.save(assignment);
    }

    /**
     * Patch assignment
     * 
     * @param assignmentId - ID of the assignment to patch
     * @param updatedFields - AssignmentModel containing fields to update
     * @return Updated AssignmentModel
     * @throws AssignmentNotFoundException if assignment not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AssignmentModel patchAssignment(String assignmentId, AssignmentModel updatedFields) {
        // Get the existing assignment
        AssignmentModel assignment = getAssignmentById(assignmentId);

        // Check if the user is authorized to edit the assignment
        userAllowedToEditAssignment(assignment);

        // If assignment exists, update only the provided fields
        if (updatedFields.getName() != null) {
            assignment.setName(updatedFields.getName());
        }
        if (updatedFields.getDescription() != null) {
            assignment.setDescription(updatedFields.getDescription());
        }
        if (updatedFields.getVisible() != null) {
            assignment.setVisible(updatedFields.getVisible());
        }
        if (updatedFields.getDueDate() != null) {
            assignment.setDueDate(updatedFields.getDueDate());
        }
        if (updatedFields.getDateUpdated() != null) {
            assignment.setDateUpdated(updatedFields.getDateUpdated());
        }
        if (updatedFields.getCourse() != null) {
            assignment.setCourse(updatedFields.getCourse());
        }

        // Save and return the updated assignment
        return assignmentRepository.save(assignment);
    }

    /**
     * Delete assignment
     * 
     * @param assignmentId - ID of the assignment to delete
     * @throws AssignmentNotFoundException if assignment not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to delete (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public void deleteAssignment(String assignmentId) {
        // Check if the user is authorized to delete the assignment
        userAllowedToEditAssignment(assignmentId);

        // Delete the assignment by ID
        assignmentRepository.deleteById(assignmentId);
    }


    // Helpers //
    /**
     * Check if user is allowed to edit the assignment
     * 
     * @param assignment - AssignmentModel to check
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    private void userAllowedToEditAssignment(AssignmentModel assignment) {
        // Check if user is allowed to edit the assignment
        courseService.userAllowedToEditCourse(assignment.getCourse());
    }

    /**
     * Check if user is allowed to edit the assignment by assignment ID
     * 
     * @param assignmentId - ID of the AssignmentModel to check
     * @throws AssignmentNotFoundException if assignment not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    private void userAllowedToEditAssignment(String assignmentId) {
        // Get the assignment by ID
        AssignmentModel assignment = getAssignmentById(assignmentId);

        // Check if user is allowed to edit the assignment
        userAllowedToEditAssignment(assignment);
    }
}
