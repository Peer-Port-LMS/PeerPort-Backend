package peerport.backend.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import peerport.backend.database.ContentRepository;
import peerport.backend.dto.content.ContentWithChildrenDTO;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.exceptions.content.ContentNotFoundException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.FileModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.model.ContentModel;
import peerport.backend.model.CourseModel;

@Service
public class ContentService {
    protected static final Logger logger = LoggerFactory.getLogger(ContentService.class);
    
    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AuthService authService;

    @Autowired
    private Validator validator;

    /**
     * Validate content model
     * 
     * @param content The content to validate
     * @throws FailedToParseFormDataException if validation fails
     */
    public void validateContent(ContentModel content) {
        logger.debug("Validating content: {}", content);

        if (content == null) {
            logger.warn("Content data is required but was null");
            throw new FailedToParseFormDataException("Content data is required.");
        }

        Set<ConstraintViolation<ContentModel>> violations = validator.validate(content);
        if (!violations.isEmpty()) {
            logger.trace("Content validation failed with {} violations", violations.size());

            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<ContentModel> violation : violations) {
                sb.append(violation.getPropertyPath().toString())
                    .append(" ")
                    .append(violation.getMessage())
                    .append("; ");
            }

            logger.warn("Content data validation failed: {}", sb.toString());
            throw new FailedToParseFormDataException("Content data validation failed: " + sb.toString());
        }

        logger.debug("Content validation passed with no violations");
    }

    /**
     * Get all content individual (without parent-child relationships)
     * @return List of ContentModel
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    public List<ContentModel> getAllContent() {
        logger.debug("Getting all content for the current user");

        // Get the users role
        UserModel user = authService.getCurrentUser();
        Role role = user.getRole();

        // Check if the user is an admin
        if (role == Role.ADMIN) {
            logger.debug("User is an admin, returning all content");
            return contentRepository.findAll();
        }

        // For non-admin users, only return content from courses they are enrolled in or instructing
        List<ContentModel> accessibleContent = new ArrayList<>();
        user.getEnrollments().forEach(enrollment -> {
            accessibleContent.addAll(enrollment.getCourse().getContent());
        });
        user.getTaughtCourses().forEach(course -> {
            accessibleContent.addAll(course.getContent());
        });

        // Return the accessible content
        logger.debug("Returning {} content items accessible to the user", accessibleContent.size());
        return accessibleContent;
    }

    /**
     * Get structured content (with parent-child relationships)
     * 
     * @return A list of ContentWithChildrenDTO representing the structured content
     */
    public List<ContentWithChildrenDTO> getStructuredContent() {
        logger.debug("Getting structured content for the current user");

        // Get the content from the repository
        List<ContentModel> contentList = getAllContent();

        // Convert to ContentWithChildrenDTO
        List<ContentWithChildrenDTO> dtoList = new ArrayList<>();
        for (ContentModel content: contentList) {
            dtoList.add(content.toContentWithChildrenDTO());
        }

        // Check for root content (content without a parent)
        List<ContentWithChildrenDTO> rootContent = new ArrayList<>();

        for (ContentWithChildrenDTO contentDTO: dtoList) {
            if (contentDTO.parentContent == null) {
                rootContent.add(contentDTO);
            }
        }

        // Return the structured content
        logger.debug("Returning structured content with {} root items", rootContent.size());
        return rootContent;
    }

    /**
     * Get content by ID
     * 
     * @param contentId The ID of the content to retrieve
     * @return The ContentModel with the specified ID
     * @throws ContentNotFoundException if content with the given ID does not exist
     */
    public ContentModel getContentById(String contentId) {
        logger.debug("Getting content by ID: {}", contentId);

        // Get the content by ID
        Optional<ContentModel> contentOpt = contentRepository.findById(contentId);

        // Check if the content exists
        if (contentOpt.isEmpty()) {
            logger.warn("Content with ID {} not found", contentId);
            throw new ContentNotFoundException(contentId);
        }
        
        ContentModel content = contentOpt.get();

        // Check if the user is allowed to access the content
        userAllowedToAccessContent(content);
        
        // Return the content
        logger.debug("Content with ID {} retrieved successfully", contentId);
        return content;
    }

    /**
     * Create content
     * 
     * @param content The content to create
     * @param courseId The ID of the course to create the content for
     * @return The created ContentModel
     * @throws CourseNotFoundException if course not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public ContentModel createContent(ContentModel content, String courseId) {
        logger.debug("Creating content for course ID: {}", courseId);

        // Get the course by ID
        CourseModel course = courseService.getCourseById(courseId);

        // Link the content to the course
        content.setCourse(course);

        // Save the content
        ContentModel saved = contentRepository.save(content);

        // Check if the user is allowed to edit the content
        userAllowedToEditContent(saved);

        // Return the saved content
        logger.debug("Content created successfully with ID: {}", saved.getContentId());
        return saved;
    }

    /**
     * Create content with files
     * 
     * @param content The content to create
     * @param files The files to associate with the content
     * @return The created ContentModel
     * @throws IOException if an error occurs during file operations
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public ContentModel createContent(ContentModel content, String courseId, List<MultipartFile> files) throws IOException {
        logger.debug("Creating content with files for course ID: {}", courseId);

        // Get the course
        CourseModel course = courseService.getCourseById(courseId);

        // Set the course to the content
        content.setCourse(course);

        // Save the content first to get an ID
        ContentModel saved = contentRepository.save(content);

        // Check if the user is allowed to edit the content
        userAllowedToEditContent(saved);
        
        // Apply file changes
        applyFileChanges(saved, files, null, null);

        // Return the saved content
        logger.debug("Content with files created successfully with ID: {}", saved.getContentId());
        return contentRepository.save(saved);
    }

    /**
     * Update content
     * 
     * @param content The existing content to update
     * @param updatedContent The new content data
     * @return The updated ContentModel 
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public ContentModel updateContent(ContentModel content, ContentModel updatedContent) {
        logger.debug("Updating content with ID: {}", content.getContentId());

        // Check if user allowed to modify content
        userAllowedToEditContent(content);

        // update fields
        content.setTitle(updatedContent.getTitle());
        content.setDescription(updatedContent.getDescription());
        content.setVisible(updatedContent.getVisible());

        // Save and return the updated content
        logger.debug("Content with ID {} updated successfully", content.getContentId());
        return contentRepository.save(content);
    }

    /**
     * Update content
     * 
     * @param contentId The ID of the content to update
     * @param updatedContent The updated content data
     * @return The updated ContentModel
     * @throws ContentNotFoundException if content with the given ID does not exist
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public ContentModel updateContent(String contentId, ContentModel updatedContent) {
        logger.debug("Updating content with ID: {}", contentId);

        // Get the content from the repository
        ContentModel content = getContentById(contentId);

        // Delegate to the core update method, which handles authorization and field updates
        logger.debug("Delegating to updateContent with the retrieved content and updated content data");
        return updateContent(content, updatedContent);
    }

    /**
     * Update content with file changes
     * 
     * @param contentId The ID of the content to update
     * @param updatedContent The updated content data
     * @param files The list of files to add or update
     * @param removeFileIds The list of file IDs to remove
     * @param replaceAll Whether to replace all existing files
     * @return The updated ContentModel
     * @throws ContentNotFoundException if content with the given ID does not exist
     * @throws IOException if an error occurs during file operations
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler) 
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public ContentModel updateContent(
        String contentId,
        ContentModel updatedContent,
        List<MultipartFile> files,
        List<String> removeFileIds,
        Boolean replaceAll
    ) throws IOException {
        logger.debug("Updating content with ID: {} with file changes", contentId);

        // Get the content from the repository
        ContentModel content = getContentById(contentId);

        // Apply file changes
        applyFileChanges(content, files, removeFileIds, replaceAll);

        // Save and return the updated content
        logger.debug("File changes applied successfully for content ID: {}", contentId);
        return updateContent(content, updatedContent);
    }

    /**
     * Patch content
     * 
     * @param contentId The ID of the content to patch
     * @param patchedContent The content data to patch
     * @return The patched ContentDTO
     * @throws ContentNotFoundException if content with the given ID does not exist
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public ContentModel patchContent(ContentModel content, ContentModel patchedContent) {
        logger.debug("Patching content with ID: {}", content.getContentId());

        // Check if user allowed to modify content
        userAllowedToEditContent(content);

        // Patch fields
        if (patchedContent.getTitle() != null) {
            content.setTitle(patchedContent.getTitle());
        }
        if (patchedContent.getDescription() != null) {
            content.setDescription(patchedContent.getDescription());
        }
        if (patchedContent.getVisible() != null) {
            content.setVisible(patchedContent.getVisible());
        }

        // Save the patched content
        logger.debug("Content with ID {} patched successfully", content.getContentId());
        return contentRepository.save(content);
    }

    /**
     * Patch content
     * 
     * @param contentId The ID of the content to patch
     * @param patchedContent The content data to patch
     * @return The patched ContentDTO
     * @throws ContentNotFoundException if content with the given ID does not exist
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public ContentModel patchContent(String contentId, ContentModel patchedContent) {
        logger.debug("Patching content with ID: {}", contentId);

        // Get the content by ID
        ContentModel content = getContentById(contentId);

        // Patch other fields
        logger.debug("Delegating to patchContent with the retrieved content and patched content data");
        return patchContent(content, patchedContent);
    }

    /**
     * Patch content with file changes
     * 
     * @param contentId The ID of the content to patch
     * @param patchedContent The content data to patch
     * @param files The list of files to add or update
     * @return The patched ContentModel
     * @throws IOException if an error occurs during file operations
     * @throws ContentNotFoundException if content with the given ID does not exist
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public ContentModel patchContent(String contentId, ContentModel patchedContent, List<MultipartFile> files) throws IOException {
        logger.debug("Patching content with ID: {} with file changes", contentId);

        // Get the content by ID
        ContentModel content = getContentById(contentId);

        // Check if the user is allowed to edit the content
        userAllowedToEditContent(content);

        // Apply file changes
        applyFileChanges(content, files, null, null);

        // Patch other fields
        logger.debug("File changes applied successfully for content ID: {}", contentId);
        return patchContent(content, patchedContent);
    }

    /**
     * Delete content by ID
     * 
     * @param contentId The ID of the content to delete
     * @throws ContentNotFoundException if content with the given ID does not exist
     * @throws UserNotAuthorizedException if user is not authorized to delete (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public void deleteContent(String contentId) {
        logger.debug("Deleting content with ID: {}", contentId);

        // Check if the user is allowed to delete the content
        // Also checks if the content exists
        userAllowedToEditContent(contentId);

        // Delete the content
        contentRepository.deleteById(contentId);
        logger.debug("Content with ID {} deleted successfully", contentId);
    }



    /**
     * Applies file changes to the content based on the provided parameters.
     * 
     * @param content The content to which file changes will be applied.
     * @param filesToAdd Files to be added to the content.
     * @param removeFileIds IDs of files to be removed from the content.
     * @param replaceAll If true, all existing files will be removed before adding new files.
     * @throws IOException If an error occurs during file operations.
     * @throws FileNotFoundException if a file to be removed does not exist.
     */
    private void applyFileChanges(
        ContentModel content,
        List<MultipartFile> filesToAdd,
        List<String> removeFileIds,
        Boolean replaceAll
    ) throws IOException{
        logger.debug("Applying file changes to content with ID: {}", content.getContentId());

        if (content.getFiles() == null) {
            content.setFiles(new ArrayList<>());
        }

        // If replaceAll is true, remove all existing files
        if (Boolean.TRUE.equals(replaceAll)) {
            logger.trace("replaceAll is true, removing all existing files from content with ID: {}", content.getContentId());

            for (FileModel existing : content.getFiles()) {
                fileService.deleteFile(existing);
            }
            content.getFiles().clear();

            logger.trace("All existing files removed successfully from content with ID: {}", content.getContentId());
        
        // If removeFileIds is provided, remove the specified files
        } else if (removeFileIds != null) {
            logger.trace("Removing files with IDs {} from content with ID: {}", removeFileIds, content.getContentId());

            List<FileModel> remaining = new ArrayList<>();
            for (FileModel existing : content.getFiles()) {
                if (removeFileIds.contains(existing.getFileId())) {
                    fileService.deleteFile(existing);
                } else {
                    remaining.add(existing);
                }
            }
            content.setFiles(remaining);

            logger.trace("Specified files removed successfully from content with ID: {}", content.getContentId());
        }

        // If filesToAdd is provided, save and add the new files
        if (filesToAdd != null) {
            logger.trace("Adding {} new files to content with ID: {}", filesToAdd.size(), content.getContentId());
            List<FileModel> savedFiles = fileService.saveContentFiles(
                filesToAdd,
                content,
                content.getCourse().getCourseId()
            );
            content.getFiles().addAll(savedFiles);
            logger.trace("New files added successfully to content with ID: {}", content.getContentId());
        }

        // Save the updated content
        contentRepository.save(content);
        logger.debug("File changes applied successfully to content with ID: {}", content.getContentId());
    }


    // Helpers //
    /**
     * Check if user is allowed to edit the content
     * @param content The ContentModel to check
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    private void userAllowedToEditContent(ContentModel content) {
        logger.debug("Checking if user is allowed to edit content with ID: {}", content.getContentId());

        // Check if the user is allowed to edit the course
        courseService.userAllowedToEditCourse(content.getCourse());
        logger.debug("User is allowed to edit content with ID: {}", content.getContentId());
    }

    /**
     * Check if user is allowed to edit the content by content ID
     * @param contentId The ID of the content to check
     * @throws ContentNotFoundException if content with the given ID does not exist
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    private void userAllowedToEditContent(String contentId) {
        logger.debug("Checking if user is allowed to edit content with ID: {}", contentId);

        // Get the content by ID
        ContentModel content = getContentById(contentId);

        // Check if the user is allowed to edit the content
        userAllowedToEditContent(content);
        logger.debug("User is allowed to edit content with ID: {}", contentId);
    }

    private void userAllowedToAccessContent(ContentModel content) {
        logger.debug("Checking if user is allowed to access content with ID: {}", content.getContentId());

        // Check if the user is allowed to access the course
        courseService.userAllowedToAccessCourse(content.getCourse());

        logger.debug("User is allowed to access content with ID: {}", content.getContentId());
    }
}
