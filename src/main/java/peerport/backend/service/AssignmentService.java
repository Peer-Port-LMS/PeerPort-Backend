package peerport.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        Set<ConstraintViolation<AssignmentModel>> violations = validator.validate(assignment);
        if (!violations.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<AssignmentModel> violation : violations) {
                errorMsg.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; ");
            }
            throw new FailedToParseFormDataException(errorMsg.toString());
        }
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
            List<FileModel> savedFiles = fileService.saveAssignmentFiles(files, savedAssignment, courseId);
            savedAssignment.getFiles().addAll(savedFiles);
            savedAssignment = assignmentRepository.save(savedAssignment);
        }
        
        // Return saved assignment
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
        // Ensure collection exists
        if (assignment.getFiles() == null) {
            assignment.setFiles(new ArrayList<>());
        }
        
        // Handle replace all
        if (replaceAll != null && replaceAll) {
            // Delete all existing files
            List<FileModel> filesToDelete = new ArrayList<>(assignment.getFiles());
            for (FileModel file : filesToDelete) {
                fileService.deleteFile(file);
            }
            assignment.getFiles().clear();
        }
        
        // Handle selective removal
        if (removeFileIds != null && !removeFileIds.isEmpty()) {
            List<FileModel> filesToRemove = assignment.getFiles().stream()
                .filter(file -> removeFileIds.contains(file.getFileId()))
                .toList();
            
            for (FileModel file : filesToRemove) {
                assignment.getFiles().remove(file);
                fileService.deleteFile(file);
            }
        }
        
        // Add new files
        if (filesToAdd != null && !filesToAdd.isEmpty()) {
            List<FileModel> newFiles = fileService.saveAssignmentFiles(
                filesToAdd,
                assignment,
                assignment.getCourse().getCourseId()
            );
            assignment.getFiles().addAll(newFiles);
        }
    }
}
