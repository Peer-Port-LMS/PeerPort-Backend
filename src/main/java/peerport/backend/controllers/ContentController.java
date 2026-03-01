package peerport.backend.controllers;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import peerport.backend.dto.content.ContentDTO;
import peerport.backend.dto.content.ContentWithAllDetailsDTO;
import peerport.backend.dto.content.ContentWithChildrenDTO;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.exceptions.content.ContentNotFoundException;
import peerport.backend.exceptions.courses.CourseNotFoundException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.ContentModel;
import peerport.backend.service.ContentService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Content Controller
 */
@RestController
@RequestMapping("/content")
public class ContentController {
    protected static final Logger logger = LoggerFactory.getLogger(ContentController.class);
    
    @Autowired
    private ContentService contentService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Get all content
     * 
     * @return ResponseEntity with the list of ContentWithChildrenDTO
     */
    @GetMapping
    public ResponseEntity<List<ContentWithChildrenDTO>> getAllContent() {
        logger.debug("Retrieving all content");

        // Get structured content
        List<ContentWithChildrenDTO> contentList = contentService.getStructuredContent();

        // Return the content list
        logger.debug("Successfully retrieved {} content items", contentList.size());
        return ResponseEntity.ok(contentList);
    }

    /**
     * Get content by ID
     * 
     * @param contentId The ID of the content to retrieve
     * @return ResponseEntity with the ContentWithAllDetailsDTO
     */
    @GetMapping("/{contentId}")
    public ResponseEntity<ContentWithAllDetailsDTO> getContentById(@PathVariable String contentId) {
        logger.debug("Retrieving content with ID: {}", contentId);

        // Get the content
        ContentModel content = contentService.getContentById(contentId);

        // Return the content if found
        logger.debug("Successfully retrieved content with ID: {}", contentId);
        return ResponseEntity.ok(content.toContentWithAllDetailsDTO());
    }

    /**
     * Creates content for a course using a JSON request body.
     *
     * @param courseId The ID of the course that will own the content.
     * @param content The content payload to create.
     * @return Response entity containing the created content DTO.
     * @throws CourseNotFoundException if the target course does not exist (Handled in GlobalExceptionHandler).
     * @throws UserNotAuthorizedException if the current user is not allowed to create content in the course (Handled in GlobalExceptionHandler).
     * @throws UserNotAuthenticatedException if the current user is not authenticated (Handled in GlobalExceptionHandler).
     */
    @PostMapping("/{courseId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<ContentDTO> createContent(@PathVariable String courseId, @RequestBody ContentModel content) {
        logger.debug("Creating content for course ID: {}", courseId);

        // Create the content
        ContentModel savedContent = contentService.createContent(content, courseId);

        // Return the created content
        logger.debug("Successfully created content with ID: {} for course ID: {}", savedContent.getContentId(), courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedContent.toDTO());
    }

    /**
     * Creates content for a course using a JSON request body.
     *
     * @param courseId The ID of the course that will own the content.
     * @param content The content payload to create.
     * @return Response entity containing the created content DTO.
     * @throws CourseNotFoundException if the target course does not exist (Handled in GlobalExceptionHandler).
     * @throws UserNotAuthorizedException if the current user is not allowed to create content in the course (Handled in GlobalExceptionHandler).
     * @throws UserNotAuthenticatedException if the current user is not authenticated (Handled in GlobalExceptionHandler).
     * @throws FailedToParseFormDataException if the JSON content data is missing or cannot be parsed into a ContentModel.
     */
    @PostMapping(path="/{courseId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<ContentDTO> createContent(
        @PathVariable String courseId,
        @RequestPart(value="content", required=false) String jsonContentFromForm,
        @RequestPart(value="files", required=false) List<MultipartFile> files
    ) throws IOException {
        logger.debug("Creating content for course ID: {} with multipart/form-data", courseId);

        // Convert json to ContentModel object
        ContentModel contentFromForm;
        try {
            contentFromForm = jsonContentFromForm != null
                ? objectMapper.readValue(jsonContentFromForm, ContentModel.class)
                : null;
        } catch (JacksonException e) {
            throw new FailedToParseFormDataException("Invalid JSON format for content data: " + jsonContentFromForm);
        }

        // Ensure content data is provided
        if (contentFromForm == null) {
            throw new FailedToParseFormDataException("Content data is required when uploading files.");
        }

        // Validate the content model
        contentService.validateContent(contentFromForm);

        // Create the content
        ContentModel savedContent = contentService.createContent(contentFromForm, courseId, files);

        // Return the created content
        logger.debug("Successfully created content with ID: {} for course ID: {}", savedContent.getContentId(), courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedContent.toDTO());
    }

    /**
     * Update content
     * 
     * @param contentId The ID of the content to update
     * @param content The content data to update
     * @return ResponseEntity with the updated ContentDTO
     * @throws ContentNotFoundException if content not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @PutMapping("/{contentId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<ContentDTO> updateContent(@PathVariable String contentId, @RequestBody ContentModel content) {
        logger.debug("Updating content with ID: {}", contentId);

        // Update the content
        ContentModel updatedContent = contentService.updateContent(contentId, content);

        // Return the DTO
        logger.debug("Successfully updated content with ID: {}", contentId);
        return ResponseEntity.ok(updatedContent.toDTO());
    }

    /**
     * Update content
     * 
     * @param contentId The ID of the content to update
     * @param jsonContentFromForm The JSON string of the content data from the form
     * @param files The list of files to upload
     * @param removeFileIds The list of file IDs to remove
     * @param replaceAll Whether to replace all existing files with the new ones
     * @return ResponseEntity with the updated ContentDTO
     * @throws IOException if file upload fails
     * @throws FailedToParseFormDataException if JSON parsing or validation fails
     * @throws ContentNotFoundException if content not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @PutMapping(path="/{contentId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<ContentDTO> updateContent(
        @PathVariable String contentId,
        @RequestPart(value="content", required=false) String jsonContentFromForm,
        @RequestPart(value="files", required=false) List<MultipartFile> files,
        @RequestPart(value="removeFileIds", required=false) List<String> removeFileIds,
        @RequestPart(value="replaceAll", required=false) Boolean replaceAll
    ) throws IOException {
        logger.debug("Updating content with ID: {} with multipart/form-data", contentId);

        // Convert json to ContentModel object
        ContentModel contentFromForm;
        try {
            contentFromForm = jsonContentFromForm != null
                ? objectMapper.readValue(jsonContentFromForm, ContentModel.class)
                : null;
        } catch (JacksonException e) {
            throw new FailedToParseFormDataException("Invalid JSON format for content data: " + jsonContentFromForm);
        }

        // Validate the content model
        if (contentFromForm != null) {
            contentService.validateContent(contentFromForm);
        }

        // Update the content
        ContentModel updated = contentService.updateContent(contentId, contentFromForm, files, removeFileIds, replaceAll);

        // Return the DTO
        logger.debug("Successfully updated content with ID: {}", contentId);
        return ResponseEntity.ok(updated.toDTO());
    }

    /**
     * Patch content
     * 
     * @param contentId The ID of the content to patch
     * @param content The content data to patch
     * @return ResponseEntity with the patched ContentDTO
     * @throws ContentNotFoundException if content not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @PatchMapping("/{contentId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<ContentDTO> patchContent(@PathVariable String contentId, @RequestBody ContentModel content) {
        logger.debug("Patching content with ID: {}", contentId);

        // Patch the content
        ContentModel patchedContent = contentService.patchContent(contentId, content);

        // Return the DTO
        logger.debug("Successfully patched content with ID: {}", contentId);
        return ResponseEntity.ok(patchedContent.toDTO());
    }

    /**
     * Patch content
     * 
     * @param contentId The ID of the content to patch
     * @param jsonContentFromForm The JSON string of the content data from the form
     * @param files The list of files to upload
     * @return ResponseEntity with the patched ContentDTO
     * @throws IOException if file upload fails
     * @throws FailedToParseFormDataException if JSON parsing or validation fails
     * @throws ContentNotFoundException if content not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @PatchMapping(path="/{contentId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<ContentDTO> patchContent(
        @PathVariable String contentId, 
        @RequestPart(value="content", required=false) String jsonContentFromForm,
        @RequestPart(value="files", required=false) List<MultipartFile> files
    ) throws IOException {
        logger.debug("Patching content with ID: {} with multipart/form-data", contentId);

        // Convert json to ContentModel object
        ContentModel contentFromForm;
        try {
            contentFromForm = jsonContentFromForm != null
                ? objectMapper.readValue(jsonContentFromForm, ContentModel.class)
                : null;
        
        // Catch JSON parsing exceptions
        } catch (JacksonException e) {
            throw new FailedToParseFormDataException("Invalid JSON format for content data: " + jsonContentFromForm);
        }
        
        // Validate the content model
        if (contentFromForm != null) {
            contentService.validateContent(contentFromForm);
        }

        // Patch the content
        ContentModel content = contentService.patchContent(contentId, contentFromForm, files);

        // Return the DTO
        logger.debug("Successfully patched content with ID: {}", contentId);
        return ResponseEntity.ok(content.toDTO());
    }

    /**
     * Delete content
     * 
     * @param contentId The ID of the content to delete
     * @return ResponseEntity with no content status
     */
    @DeleteMapping("/{contentId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<Void> deleteContent(@PathVariable String contentId) {
        logger.debug("Deleting content with ID: {}", contentId);
        
        // Delete the content
        contentService.deleteContent(contentId);

        // Return no content response
        logger.debug("Successfully deleted content with ID: {}", contentId);
        return ResponseEntity.noContent().build();
    }
}
