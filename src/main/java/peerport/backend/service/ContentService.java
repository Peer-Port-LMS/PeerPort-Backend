package peerport.backend.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        if (content == null) {
            throw new FailedToParseFormDataException("Content data is required.");
        }

        Set<ConstraintViolation<ContentModel>> violations = validator.validate(content);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<ContentModel> violation : violations) {
                sb.append(violation.getPropertyPath().toString())
                    .append(" ")
                    .append(violation.getMessage())
                    .append("; ");
            }
            throw new FailedToParseFormDataException("Content data validation failed: " + sb.toString());
        }
    }

    /**
     * Get all content individual (without parent-child relationships)
     * @return List of ContentModel
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    public List<ContentModel> getAllContent() {
        // Get the users role
        UserModel user = authService.getCurrentUser();
        Role role = user.getRole();

        // Check if the user is an admin
        if (role == Role.ADMIN) return contentRepository.findAll();

        // For non-admin users, only return content from courses they are enrolled in or instructing
        List<ContentModel> accessibleContent = new ArrayList<>();
        user.getEnrollments().forEach(enrollment -> {
            accessibleContent.addAll(enrollment.getCourse().getContent());
        });
        user.getTaughtCourses().forEach(course -> {
            accessibleContent.addAll(course.getContent());
        });

        // Return the accessible content
        return accessibleContent;
    }

    /**
     * Get structured content (with parent-child relationships)
     * 
     * @return A list of ContentWithChildrenDTO representing the structured content
     */
    public List<ContentWithChildrenDTO> getStructuredContent() {
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
        // Get the content by ID
        Optional<ContentModel> contentOpt = contentRepository.findById(contentId);

        // Check if the content exists
        if (contentOpt.isEmpty()) {
            throw new ContentNotFoundException(contentId);
        }
        
        ContentModel content = contentOpt.get();

        // Check if the user is allowed to access the content
        userAllowedToAccessContent(content);
        
        // Return the content
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
        // Get the course by ID
        CourseModel course = courseService.getCourseById(courseId);

        // Link the content to the course
        content.setCourse(course);

        // Save the content
        ContentModel saved = contentRepository.save(content);

        // Check if the user is allowed to edit the content
        userAllowedToEditContent(saved);

        // Return the saved content
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
        // Check if user allowed to modify content
        userAllowedToEditContent(content);

        // update fields
        content.setTitle(updatedContent.getTitle());
        content.setDescription(updatedContent.getDescription());
        content.setVisible(updatedContent.getVisible());

        // Save and return the updated content
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
        // Get the content from the repository
        ContentModel content = getContentById(contentId);

        // Delegate to the core update method, which handles authorization and field updates
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
        // Get the content from the repository
        ContentModel content = getContentById(contentId);

        // Apply file changes
        applyFileChanges(content, files, removeFileIds, replaceAll);

        // Save and return the updated content
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
        // Get the content by ID
        ContentModel content = getContentById(contentId);

        // Patch other fields
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
        // Get the content by ID
        ContentModel content = getContentById(contentId);

        // Check if the user is allowed to edit the content
        userAllowedToEditContent(content);

        // Apply file changes
        applyFileChanges(content, files, null, null);

        // Patch other fields
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
        // Check if the user is allowed to delete the content
        // Also checks if the content exists
        userAllowedToEditContent(contentId);

        // Delete the content
        contentRepository.deleteById(contentId);
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
        if (content.getFiles() == null) {
            content.setFiles(new ArrayList<>());
        }

        if (Boolean.TRUE.equals(replaceAll)) {
            for (FileModel existing : content.getFiles()) {
                fileService.deleteFile(existing);
            }
            content.getFiles().clear();
        } else if (removeFileIds != null) {
            List<FileModel> remaining = new ArrayList<>();
            for (FileModel existing : content.getFiles()) {
                if (removeFileIds.contains(existing.getFileId())) {
                    fileService.deleteFile(existing);
                } else {
                    remaining.add(existing);
                }
            }
            content.setFiles(remaining);
        }

        if (filesToAdd != null) {
            List<FileModel> savedFiles = fileService.saveContentFiles(
                filesToAdd,
                content,
                content.getCourse().getCourseId()
            );
            content.getFiles().addAll(savedFiles);
        }

        // Save the updated content
        contentRepository.save(content);
    }


    // Helpers //
    /**
     * Check if user is allowed to edit the content
     * @param content The ContentModel to check
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    private void userAllowedToEditContent(ContentModel content) {
        // Check if the user is allowed to edit the course
        courseService.userAllowedToEditCourse(content.getCourse());
    }

    /**
     * Check if user is allowed to edit the content by content ID
     * @param contentId The ID of the content to check
     * @throws ContentNotFoundException if content with the given ID does not exist
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    private void userAllowedToEditContent(String contentId) {
        // Get the content by ID
        ContentModel content = getContentById(contentId);

        // Check if the user is allowed to edit the content
        userAllowedToEditContent(content);
    }

    private void userAllowedToAccessContent(ContentModel content) {
        // Check if the user is allowed to access the course
        courseService.userAllowedToAccessCourse(content.getCourse());
    }
}
