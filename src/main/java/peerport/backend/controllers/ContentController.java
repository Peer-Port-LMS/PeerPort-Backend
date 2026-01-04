package peerport.backend.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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

import peerport.backend.dto.ContentDTO;
import peerport.backend.dto.ContentWithAllDetailsDTO;
import peerport.backend.dto.ContentWithChildrenDTO;
import peerport.backend.exceptions.FailedToParseFormDataException;
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
        // Get structured content
        List<ContentWithChildrenDTO> contentList = contentService.getStructuredContent();

        // Return the content list
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
        // Get the content
        ContentModel content = contentService.getContentById(contentId);

        // Return the content if found
        return ResponseEntity.ok(content.toContentWithAllDetailsDTO());
    }

    @PostMapping("/{courseId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<ContentDTO> createContent(@PathVariable String courseId, @RequestBody ContentModel content) {
        // Create the content
        ContentModel savedContent = contentService.createContent(content, courseId);

        // Return the created content
        return ResponseEntity.status(HttpStatus.CREATED).body(savedContent.toDTO());
    }

    @PostMapping(path="/{courseId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<ContentDTO> createContent(
        @PathVariable String courseId,
        @RequestPart(value="content", required=false) String jsonContentFromForm,
        @RequestPart(value="files", required=false) List<MultipartFile> files
    ) throws IOException {
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
        // Update the content
        ContentModel updatedContent = contentService.updateContent(contentId, content);

        // Return the DTO
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
        // Patch the content
        ContentModel patchedContent = contentService.patchContent(contentId, content);

        // Return the DTO
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
        // Delete the content
        contentService.deleteContent(contentId);

        // Return no content response
        return ResponseEntity.noContent().build();
    }
}
