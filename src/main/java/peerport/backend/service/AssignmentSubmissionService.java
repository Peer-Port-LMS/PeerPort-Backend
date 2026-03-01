package peerport.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    protected static final Logger logger = LoggerFactory.getLogger(AssignmentSubmissionService.class);

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
        logger.debug("Getting all assignment submissions for the current user.");

        // Get the current user and role
        UserModel user = authService.getCurrentUser();
        Role role = user.getRole();

        // If admin return all
        if (role == Role.ADMIN) {
            logger.debug("User is admin, returning all assignment submissions.");
            return assignmentSubmissionRepository.findAll();
        }

        // Collect submissions based on role
        List<AssignmentSubmissionModel> submissions = new ArrayList<>();

        // Instructors: get all submissions for courses they teach
        if (role == Role.INSTRUCTOR) {
            logger.trace("User is instructor, collecting submissions for courses they teach.");
            
            user.getTaughtCourses().forEach(course ->
                course.getAssignments().forEach(assignment -> {
                    submissions.addAll(assignment.getSubmissions());
                })
            );
        } else {
            logger.trace("User is student, collecting their own submissions.");

            // Students (and other non-admin, non-instructor roles): only their own submissions
            user.getEnrollments().forEach(enrollment ->
                enrollment.getCourse().getAssignments().forEach(assignment ->
                    assignment.getSubmissions().forEach(submission -> {
                        if (submission.getUser() != null && submission.getUser().equals(user)) {
                            submissions.add(submission);
                        }
                    })
                )
            );
        }

        // Deduplicate submissions by ID
        List<AssignmentSubmissionModel> uniqueSubmissions = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (AssignmentSubmissionModel submission : submissions) {
            // Get the assignment submission by Id
            String id = submission.getAssignmentSubmissionId();
            if (seenIds.add(id)) {
                logger.trace("Adding submission with ID {} to unique submissions list.", id);
                uniqueSubmissions.add(submission);
            } else {
                logger.trace("Skipping duplicate submission with ID {}.", id);
            }
        }

        // Return the submissions
        logger.debug("Returning {} unique assignment submissions for the current user.", uniqueSubmissions.size());
        return uniqueSubmissions;
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
        logger.debug("Getting assignment submission by ID: {}", assignmentSubmissionId);

        // Get the assignment submission by ID
        Optional<AssignmentSubmissionModel> assignmentSubmissionOpt = assignmentSubmissionRepository.findById(assignmentSubmissionId);

        // Check if it exists
        if (assignmentSubmissionOpt.isEmpty()) {
            logger.warn("Assignment submission with ID {} not found.", assignmentSubmissionId);
            throw new AssignmentSubmissionNotFoundException(assignmentSubmissionId);
        }

        // Check if the user is allowed to view the submission
        userAllowedToModifySubmission(assignmentSubmissionOpt.get());

        // Return the assignment submission
        logger.debug("Returning assignment submission with ID: {}", assignmentSubmissionId);
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
        logger.debug("Creating new assignment submission for assignment ID: {}", assignment.getAssignmentId());

        // Get the user
        UserModel user = authService.getCurrentUser();

        // Check if the user is allowed to submit to the assignment
        userAllowedToModifySubmission(submission);
        
        // Link the submission to the assignment
        submission.setAssignment(assignment);

        // Link the submission to the user
        submission.setUser(user);

        // Save the submission
        logger.debug("Saving new assignment submission for user ID: {} and assignment ID: {}", user.getUserId(), assignment.getAssignmentId());
        AssignmentSubmissionModel savedSubmission = assignmentSubmissionRepository.save(submission);
        return assignmentSubmissionRepository.save(savedSubmission);
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
        logger.debug("Creating new assignment submission for assignment ID: {}", assignmentId);

        // Get the assignment
        AssignmentModel assignment = assignmentService.getAssignmentById(assignmentId);

        // Save the submission
        logger.debug("Saving new assignment submission for assignment ID: {}", assignmentId);
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
        logger.debug("Creating new assignment submission with files for assignment ID: {}", assignmentId);

        // Check the files and add them to the submission
        fileService.checkFileSizes(files);
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
            logger.trace("Saving files for assignment submission ID: {}", savedSubmission.getAssignmentSubmissionId());

            savedSubmission.setSubmittedFiles(
                fileService.saveAssignmentSubmissionFiles(files, assignment, savedSubmission)
            );
            
            // Persist the relationship between the submission and its files
            savedSubmission = assignmentSubmissionRepository.save(savedSubmission);
            logger.trace("Files saved and linked to assignment submission ID: {}", savedSubmission.getAssignmentSubmissionId());
        }

        logger.debug("Created new assignment submission with ID: {} for assignment ID: {}", savedSubmission.getAssignmentSubmissionId(), assignmentId);
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
        logger.debug("Deleting assignment submission with ID: {}", assignmentSubmissionId);

        // Check if the submission exists and if the user is allowed to modify it
        getSubmissionById(assignmentSubmissionId);

        // Delete the assignment submission
        assignmentSubmissionRepository.deleteById(assignmentSubmissionId);
        logger.debug("Deleted assignment submission with ID: {}", assignmentSubmissionId);
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
        logger.debug("Checking if current user is allowed to modify submission with ID: {}", submission.getAssignmentSubmissionId());

        // Get the current user's role
        UserModel currentUser = authService.getCurrentUser();
        Role role = currentUser.getRole();

        if (role == Role.ADMIN) {
            logger.debug("User is admin, allowed to modify any submission.");
            return;
        }
        
        // If the user is an instructor allow them to create or delete a submission
        if (role == Role.INSTRUCTOR) {
            logger.trace("User is instructor, checking if they teach the course for the submission's assignment.");

            // Check if the instructor teaches the course the assignment belongs to
            if (!currentUser.getTaughtCourses().contains(submission.getAssignment().getCourse())) {
                logger.warn("Instructor user ID: {} is not authorized to modify submission ID: {} because they do not teach the course.", currentUser.getUserId(), submission.getAssignmentSubmissionId());
                throw new UserNotAuthorizedException("You are not authorized to modify this submission.");
            }

            logger.debug("Instructor user ID: {} is authorized to modify submission ID: {}.", currentUser.getUserId(), submission.getAssignmentSubmissionId());
            return;
        }

        // If the user is a student only allow them to modify their own submissions
        if (!submission.getUser().getUserId().equals(currentUser.getUserId())) {
            logger.warn("Student user ID: {} is not authorized to modify submission ID: {} because they do not own the submission.", currentUser.getUserId(), submission.getAssignmentSubmissionId());
            throw new UserNotAuthorizedException("You are not authorized to modify this submission.");
        }

        logger.debug("Student user ID: {} is authorized to modify their own submission ID: {}.", currentUser.getUserId(), submission.getAssignmentSubmissionId());
    }
}
