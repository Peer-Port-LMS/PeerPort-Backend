package peerport.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import peerport.backend.database.AssignmentsRepository;
import peerport.backend.exceptions.assignments.AssignmentNotFoundException;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.AssignmentModel;
import peerport.backend.model.CourseModel;
import peerport.backend.model.FileModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;

@Service
public class AssignmentService {
    protected static final Logger logger = LogManager.getLogger();
    
    @Autowired
    private AuthService authService;

    @Autowired
    private AssignmentsRepository assignmentRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private FileService fileService;

    @Autowired
    private Validator validator;

    @Value("${file.upload-size-limit}")
    private long fileUploadSizeLimit;

    /**
     * Validate assignment model
     * 
     * @param assignment - The assignment to validate
     * @throws FailedToParseFormDataException if validation fails
     */
    public void validateAssignment(AssignmentModel assignment) {
        logger.debug("Validating assignment model: {}", assignment);

        Set<ConstraintViolation<AssignmentModel>> violations = validator.validate(assignment);
        if (!violations.isEmpty()) {
            logger.trace("Validation failed for assignment model: {}. Violations: {}", assignment, violations);

            StringBuilder errorMsg = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<AssignmentModel> violation : violations) {
                errorMsg.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; ");
            }

            logger.warn("Assignment model validation failed: {}", errorMsg.toString());
            throw new FailedToParseFormDataException(errorMsg.toString());
        }

        logger.debug("Assignment model validation passed: {}", assignment);
    }

    /**
     * Get all assignments
     * 
     * @return List of AssignmentModels
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to view (Handled in GlobalExceptionHandler)
     */
    public List<AssignmentModel> getAllAssignments() {
        logger.debug("Getting all assignments for current user.");

        // Get the users role
        UserModel user = authService.getCurrentUser();

        // Check user role
        if (user.getRole() == Role.ADMIN) {
            logger.debug("User is admin, returning all assignments.");
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
        logger.debug("Returning {} assignments for user ID: {}", assignments.size(), user.getUserId());
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
        logger.debug("Getting assignment by ID: {}", assignmentId);

        // Get the assignment by ID
        Optional<AssignmentModel> assignment = assignmentRepository.findById(assignmentId);

        // Check if the assignment exists or not
        if (assignment.isEmpty()) {
            logger.warn("Assignment not found with ID: {}", assignmentId);
            throw new AssignmentNotFoundException(assignmentId);
        }

        AssignmentModel assignmentModel = assignment.get();

        userAllowedToAccessAssignment(assignmentModel);

        // Return the assignment
        logger.debug("Returning assignment with ID: {}", assignmentId);
        return assignmentModel;
    }

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
        logger.debug("Creating new assignment for course ID: {}. Assignment details: {}", courseId, assignment);

        // Get the course by ID
        CourseModel course = courseService.getCourseById(courseId);

        // Check if the user is allowed to edit the course
        courseService.userAllowedToEditCourse(course);
        
        // Set the course to the assignment
        assignment.setCourse(course);

        // Return saved assignment
        logger.debug("Saving new assignment for course ID: {}. Assignment details: {}", courseId, assignment);
        return assignmentRepository.save(assignment);
    }

    /**
     * Create assignment with files
     * 
     * @param assignment - AssignmentModel to create
     * @param courseId - ID of the course to create the assignment for
     * @param files - List of files to attach to the assignment
     * @return The created AssignmentModel
     * @throws IOException If there was an error saving the files
     * @throws FileSizeLimitExceededException if any file exceeds size limit
     * @throws CourseNotFoundException if course not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AssignmentModel createAssignment(AssignmentModel assignment, String courseId, List<MultipartFile> files) throws IOException {
        logger.debug("Creating new assignment with files for course ID: {}. Assignment details: {}. Number of files: {}", courseId, assignment, files != null ? files.size() : 0);

        // Validate file sizes
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && file.getSize() > fileUploadSizeLimit) {
                    throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
                }
            }
        }
        
        // Get the course by ID
        CourseModel course = courseService.getCourseById(courseId);
        
        // Check if the user is allowed to edit the course
        courseService.userAllowedToEditCourse(course);
        
        // Set the course to the assignment
        assignment.setCourse(course);
        
        // Save the assignment first to get ID for file naming
        AssignmentModel savedAssignment = assignmentRepository.save(assignment);
        
        // Add files if provided
        if (files != null && !files.isEmpty()) {
            logger.trace("Saving {} files for assignment ID: {}.", files.size(), savedAssignment.getAssignmentId());
            List<FileModel> savedFiles = fileService.saveAssignmentFiles(files, savedAssignment, courseId);
            savedAssignment.getFiles().addAll(savedFiles);
            savedAssignment = assignmentRepository.save(savedAssignment);
            logger.trace("Files saved and linked to assignment ID: {}.", savedAssignment.getAssignmentId());
        }
        
        // Return saved assignment
        logger.debug("Created new assignment with ID: {} for course ID: {}.", savedAssignment.getAssignmentId(), courseId);
        return savedAssignment;
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
        logger.debug("Updating assignment with ID: {}. Updated details: {}", assignmentId, updatedAssignment);

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
        logger.debug("Saving updated assignment with ID: {}. Updated details: {}", assignmentId, assignment);
        return assignmentRepository.save(assignment);
    }

    /**
     * Update assignment with file changes
     * 
     * @param assignmentId - ID of the assignment to update
     * @param updatedAssignment - AssignmentModel containing updated fields
     * @param files - List of files to attach to the assignment
     * @param removeFileIds - List of file IDs to remove
     * @param replaceAll - Whether to replace all existing files
     * @return Updated AssignmentModel
     * @throws IOException If there was an error saving the files
     * @throws FileSizeLimitExceededException if any file exceeds size limit
     * @throws AssignmentNotFoundException if assignment not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AssignmentModel updateAssignment(
        String assignmentId,
        AssignmentModel updatedAssignment,
        List<MultipartFile> files,
        List<String> removeFileIds,
        Boolean replaceAll
    ) throws IOException {
        logger.debug("Updating assignment with ID: {} with file changes. Updated details: {}. Number of files to add: {}. Number of files to remove: {}. Replace all files: {}", 
            assignmentId, 
            updatedAssignment, 
            files != null ? files.size() : 0, 
            removeFileIds != null ? removeFileIds.size() : 0, 
            replaceAll != null ? replaceAll : false
        );

        // Validate file sizes
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && file.getSize() > fileUploadSizeLimit) {
                    throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
                }
            }
        }
        
        // Get the assignment
        AssignmentModel assignment = getAssignmentById(assignmentId);
        
        // Check if the user is authorized to edit the assignment
        userAllowedToEditAssignment(assignment);
        
        // Apply file changes
        applyFileChanges(assignment, files, removeFileIds, replaceAll);
        
        // Update the fields
        assignment.setName(updatedAssignment.getName());
        assignment.setDescription(updatedAssignment.getDescription());
        assignment.setVisible(updatedAssignment.getVisible());
        assignment.setDueDate(updatedAssignment.getDueDate());
        
        // Return the saved assignment
        logger.debug("Saving updated assignment with ID: {} after applying file changes. Updated details: {}", assignmentId, assignment);
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
        logger.debug("Patching assignment with ID: {}. Updated fields: {}", assignmentId, updatedFields);

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
        logger.debug("Saving patched assignment with ID: {}. Patched details: {}", assignmentId, assignment);
        return assignmentRepository.save(assignment);
    }

    /**
     * Patch assignment with file changes
     * 
     * @param assignmentId - ID of the assignment to patch
     * @param updatedFields - AssignmentModel containing fields to update
     * @param files - List of files to attach to the assignment
     * @param removeFileIds - List of file IDs to remove
     * @param replaceAll - Whether to replace all existing files
     * @return Updated AssignmentModel
     * @throws IOException If there was an error saving the files
     * @throws FileSizeLimitExceededException if any file exceeds size limit
     * @throws AssignmentNotFoundException if assignment not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AssignmentModel patchAssignment(
        String assignmentId,
        AssignmentModel updatedFields,
        List<MultipartFile> files,
        List<String> removeFileIds,
        Boolean replaceAll
    ) throws IOException {
        logger.debug("Patching assignment with ID: {} with file changes. Updated fields: {}. Number of files to add: {}. Number of files to remove: {}. Replace all files: {}", 
            assignmentId, 
            updatedFields, 
            files != null ? files.size() : 0, 
            removeFileIds != null ? removeFileIds.size() : 0, 
            replaceAll != null ? replaceAll : false
        );
        
        // Validate file sizes
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && file.getSize() > fileUploadSizeLimit) {
                    throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
                }
            }
        }
        
        // Get the existing assignment
        AssignmentModel assignment = getAssignmentById(assignmentId);
        
        // Check if the user is authorized to edit the assignment
        userAllowedToEditAssignment(assignment);
        
        // Apply file changes first
        applyFileChanges(assignment, files, removeFileIds, replaceAll);
        
        // Save and return the updated assignment
        logger.debug("Saving patched assignment with ID: {} after applying file changes. Patched details: {}", assignmentId, assignment);
        return patchAssignment(assignmentId, updatedFields);
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
        logger.debug("Deleting assignment with ID: {}", assignmentId);

        // Check if the user is authorized to delete the assignment
        userAllowedToEditAssignment(assignmentId);

        // Delete the assignment by ID
        logger.debug("Deleting assignment with ID: {} from repository.", assignmentId);
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
        logger.debug("Checking if user is allowed to edit assignment with ID: {}.", assignment.getAssignmentId());

        // Check if user is allowed to edit the assignment
        courseService.userAllowedToEditCourse(assignment.getCourse());
        logger.debug("User is allowed to edit assignment with ID: {}.", assignment.getAssignmentId());
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
        logger.debug("Checking if user is allowed to edit assignment with ID: {}.", assignmentId);

        // Get the assignment by ID
        AssignmentModel assignment = getAssignmentById(assignmentId);

        // Check if user is allowed to edit the assignment
        userAllowedToEditAssignment(assignment);
        logger.debug("User is allowed to edit assignment with ID: {}.", assignmentId);
    }

    private void userAllowedToAccessAssignment(AssignmentModel assingment) {
        logger.debug("Checking if user is allowed to access assignment with ID: {}.", assingment.getAssignmentId());

        // Check if user is allowed to access the assignment
        courseService.userAllowedToAccessCourse(assingment.getCourse());
        logger.debug("User is allowed to access assignment with ID: {}.", assingment.getAssignmentId());
    }

    /**
     * Apply file changes to assignment
     * 
     * @param assignment - The assignment to apply changes to
     * @param filesToAdd - The files to add
     * @param removeFileIds - The IDs of files to remove
     * @param replaceAll - Whether to replace all existing files
     * @throws IOException If there was an error saving the files
     */
    private void applyFileChanges(
        AssignmentModel assignment,
        List<MultipartFile> filesToAdd,
        List<String> removeFileIds,
        Boolean replaceAll
    ) throws IOException {
        logger.debug("Applying file changes to assignment with ID: {}. Number of files to add: {}. Number of files to remove: {}. Replace all files: {}", 
            assignment.getAssignmentId(), 
            filesToAdd != null ? filesToAdd.size() : 0, 
            removeFileIds != null ? removeFileIds.size() : 0, 
            replaceAll != null ? replaceAll : false
        );

        // Ensure collection exists
        if (assignment.getFiles() == null) {
            assignment.setFiles(new ArrayList<>());
        }
        
        // Handle replace all
        if (replaceAll != null && replaceAll) {
            logger.trace("Replace all is true, removing all existing files for assignment ID: {}.", assignment.getAssignmentId());

            // Delete all existing files
            List<FileModel> filesToDelete = new ArrayList<>(assignment.getFiles());
            for (FileModel file : filesToDelete) {
                fileService.deleteFile(file);
            }
            assignment.getFiles().clear();

            logger.trace("All existing files removed for assignment ID: {}.", assignment.getAssignmentId());
        }
        
        // Handle selective removal
        if (removeFileIds != null && !removeFileIds.isEmpty()) {
            logger.trace("Removing specified files from assignment ID: {}. File IDs to remove: {}", assignment.getAssignmentId(), removeFileIds);

            List<FileModel> filesToRemove = assignment.getFiles().stream()
                .filter(file -> removeFileIds.contains(file.getFileId()))
                .toList();
            
            for (FileModel file : filesToRemove) {
                assignment.getFiles().remove(file);
                fileService.deleteFile(file);
            }

            logger.trace("Specified files removed from assignment ID: {}. File IDs removed: {}", assignment.getAssignmentId(), removeFileIds);
        }
        
        // Add new files
        if (filesToAdd != null && !filesToAdd.isEmpty()) {
            logger.trace("Adding new files to assignment ID: {}. Number of files to add: {}", assignment.getAssignmentId(), filesToAdd.size());
            List<FileModel> newFiles = fileService.saveAssignmentFiles(
                filesToAdd,
                assignment,
                assignment.getCourse().getCourseId()
            );
            assignment.getFiles().addAll(newFiles);
            logger.trace("New files added to assignment ID: {}. Number of files added: {}", assignment.getAssignmentId(), newFiles.size());
        }

        logger.debug("File changes applied to assignment with ID: {}.", assignment.getAssignmentId());
    }
}
