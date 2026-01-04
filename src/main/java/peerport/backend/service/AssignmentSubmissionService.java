package peerport.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import peerport.backend.database.AssignmentSubmissionRepository;
import peerport.backend.exceptions.assignmentSubmissions.AssignmentSubmissionNotFoundException;
import peerport.backend.exceptions.assignments.AssignmentNotFoundException;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.exceptions.users.UserNotFoundException;
import peerport.backend.model.AssignmentModel;
import peerport.backend.model.AssignmentSubmissionModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;

@Service
public class AssignmentSubmissionService {

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AuthService authService;

    @Autowired
    private FileService fileService;


    @Value("${file.upload-size-limit}")
    private long fileUploadSizeLimit;



    /**
     * Get all assignment submissions
     * 
     * @return List of all assignment submission models
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     */
    public List<AssignmentSubmissionModel> getAllAssignmentSubmissions() {
        // Get the current user and role
        UserModel user = authService.getCurrentUser();
        Role role = user.getRole();

        // If admin return all
        if (role == Role.ADMIN) return assignmentSubmissionRepository.findAll();

        // If user is an instructor or student return all submissions for their courses
        // Get the submissions for the instructors courses
        List<AssignmentSubmissionModel> submissions = new ArrayList<>();
        user.getTaughtCourses().forEach(course -> 
            course.getAssignments().forEach(assignment -> {
                submissions.addAll(assignment.getSubmissions());
            })
        );
        user.getEnrollments().forEach(enrollment -> {
            enrollment.getCourse().getAssignments().forEach(assignment -> {
                submissions.addAll(assignment.getSubmissions());
            });
        });

        // Return the submissions
        return submissions;
    }

    /**
     * Get assignment submission by ID
     * 
     * @param assignmentSubmissionId The ID of the assignment submission
     * @return The assignment submission model
     * @throws AssignmentSubmissionNotFoundException If the assignment submission is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to view the submission (Handled in GlobalExceptionHandler)
     */
    public AssignmentSubmissionModel getSubmissionById(String assignmentSubmissionId) {
        // Get the assignment by Id
        Optional<AssignmentSubmissionModel> assignmentSubmissionOpt = assignmentSubmissionRepository.findById(assignmentSubmissionId);

        // Check if it exists
        if (assignmentSubmissionOpt.isEmpty()) {
            throw new AssignmentSubmissionNotFoundException(assignmentSubmissionId);
        }

        // Check if the user is allowed to view the submission
        userAllowedToModifySubmission(assignmentSubmissionOpt.get());

        // Return the assignment submission
        return assignmentSubmissionOpt.get();
    }

    /**
     * Create a new assignment submission
     * 
     * @param submission The assignment submission model to create
     * @param assignment The assignment model to link the submission to
     * @return The created assignment submission model
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to submit to the assignment (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AssignmentSubmissionModel createAssignmentSubmission(AssignmentSubmissionModel submission, AssignmentModel assignment) {
        // Get the user
        UserModel user = authService.getCurrentUser();
        
        // Link the submission to the assignment
        submission.setAssignment(assignment);

        // Link the submission to the user
        submission.setUser(user);

        // Check if the user is allowed to submit to the assignment
        userAllowedToModifySubmission(submission);

        // Save the submission
        return assignmentSubmissionRepository.save(submission);
    }

    /**
     * Create a new assignment submission
     * 
     * @param submission The assignment submission model to create
     * @param assignmentId The ID of the assignment to link the submission to
     * @return The created assignment submission model
     * @throws AssignmentNotFoundException If the assignment is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to submit to the assignment (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AssignmentSubmissionModel createAssignmentSubmission(AssignmentSubmissionModel submission, String assignmentId) {
        // Get the assignment
        AssignmentModel assignment = assignmentService.getAssignmentById(assignmentId);

        // Save the submission
        return createAssignmentSubmission(submission, assignment);
    }

    /**
     * Create a new assignment submission with files
     * 
     * @param submission The assignment submission model to create
     * @param assignmentId The ID of the assignment to link the submission to
     * @param files The files to upload with the submission
     * @return The created assignment submission model
     * @throws IOException If there is an error uploading the files
     * @throws FileSizeLimitExceededException If any of the files exceed the size limit (Handled in GlobalExceptionHandler)
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws AssignmentNotFoundException If the assignment is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to submit to the assignment (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AssignmentSubmissionModel createAssignmentSubmission(
        AssignmentSubmissionModel submission, 
        String assignmentId, 
        List<MultipartFile> files
    ) throws IOException {
        // Check the files and add them to the submission
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && file.getSize() > fileUploadSizeLimit) {
                    throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
                }
            }
        }

        // Get the assignment
        AssignmentModel assignment = assignmentService.getAssignmentById(assignmentId);

        // First, save the submission so that it has an ID which can be used to link files
        AssignmentSubmissionModel savedSubmission = createAssignmentSubmission(submission, assignment);

        // Save the assignment submission files now that the submission has an ID
        if (files != null && !files.isEmpty()) {
            savedSubmission.setSubmittedFiles(
                fileService.saveAssignmentSubmissionFiles(files, assignment, savedSubmission)
            );
            // Persist the relationship between the submission and its files
            savedSubmission = assignmentSubmissionRepository.save(savedSubmission);
        }

        return savedSubmission;
    }

    /**
     * Delete assignment submission by ID
     * 
     * @param assignmentSubmissionId The ID of the assignment submission to delete
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to modify the submission (Handled in GlobalExceptionHandler)
     * @throws AssignmentSubmissionNotFoundException If the assignment submission is not found (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public void deleteAssignmentSubmissionById(String assignmentSubmissionId) {
        // Check if the submission exists and if the user is allowed to modify it
        getSubmissionById(assignmentSubmissionId);

        // Delete the assignment submission
        assignmentSubmissionRepository.deleteById(assignmentSubmissionId);
    }


    // Helpers //
    /**
     * Check if the current user is allowed to modify the given submission
     * 
     * @param submission The assignment submission model to check
     * @throws UserNotFoundException If the user is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to modify the submission (Handled in GlobalExceptionHandler)
     */
    private void userAllowedToModifySubmission(AssignmentSubmissionModel submission) {
        // Get the current users role
        UserModel currentUser = authService.getCurrentUser();
        Role role = currentUser.getRole();

        if (role == Role.ADMIN) return;
        
        // If the user is an instructor allow them to create or delete a submission
        if (role == Role.INSTRUCTOR) {
            // Check if the instructor teaches the course the assignment belongs to
            if (!currentUser.getTaughtCourses().contains(submission.getAssignment().getCourse())) {
                throw new UserNotAuthorizedException("You are not authorized to modify this submission.");
            }
            return;
        }

        // If the user is a student only allow them to modify their own submissions
        if (!submission.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UserNotAuthorizedException("You are not authorized to modify this submission.");
        }

    }
}
