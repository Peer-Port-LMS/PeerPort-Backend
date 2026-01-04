package peerport.backend.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import peerport.backend.dto.announcements.AnnouncementDTO;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.exceptions.announcements.AnnouncementNotFoundException;
import peerport.backend.exceptions.courses.CourseNotFoundException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.AnnouncementModel;
import peerport.backend.service.AnnouncementService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
    
    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private Validator validator;

    /**
     * Get all announcements
     * 
     * @return List of AnnouncementDTO
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     */
    @GetMapping
    public ResponseEntity<List<AnnouncementDTO>> getAllAnnouncements() {
        // Get all announcements
        List<AnnouncementModel> announcements = announcementService.getAllAnnouncements();

        // Convert to DTOs
        List<AnnouncementDTO> announcementDTOs = new ArrayList<>();
        for (AnnouncementModel announcement : announcements) {
            announcementDTOs.add(announcement.toDTO());
        }

        // Return the DTOs
        return ResponseEntity.ok(announcementDTOs);
    }

    /**
     * Get announcement by ID
     * 
     * @param announcementId - ID of the announcement to get
     * @return The AnnouncementModel
     * @throws AnnouncementNotFoundException If the announcement with the given ID does not exist
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     */
    @GetMapping("/{announcementId}")
    public ResponseEntity<AnnouncementDTO> getAnnouncementById(@PathVariable String announcementId) {
        // Get the announcement by ID
        AnnouncementModel announcement = announcementService.getAnnouncementById(announcementId);

        // Return the announcement as DTO
        return ResponseEntity.ok(announcement.toDTO());
    }

    /**
     * Create announcement
     * 
     * @param courseId - ID of the course to create the announcement for
     * @param announcementModel - AnnouncementModel to create
     * @return The created AnnouncementModel
     * @throws CourseNotFoundException If the course with the given ID does not exist
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to create this announcement
     */
    @PostMapping("/{courseId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<AnnouncementDTO> createAnnouncement(
        @PathVariable String courseId, 
        @Valid @RequestBody AnnouncementModel announcementModel
    ) {
        // Create announcement
        AnnouncementModel createdAnnouncement = announcementService.createAnnouncement(courseId, announcementModel);

        // Return the created announcement
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAnnouncement.toDTO());
    }

    /**
     * Create announcement
     * 
     * @param courseId - ID of the course to create the announcement for
     * @param announcementModel - AnnouncementModel to create
     * @return The created AnnouncementModel
     * @throws CourseNotFoundException If the course with the given ID does not exist
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to create this announcement
     */
    @PostMapping(path="/{courseId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<AnnouncementDTO> createAnnouncement(
        @PathVariable String courseId, 
        @RequestPart(value="announcement", required=false) String jsonAnnouncementModel,
        @RequestPart(value="files", required=false) List<MultipartFile> files
    ) throws IOException {
        // Convert json to AnnouncementModel object
        AnnouncementModel announcementFromForm;
        try {
            announcementFromForm = objectMapper.readValue(jsonAnnouncementModel, AnnouncementModel.class);
        
        // Catch JSON parsing exceptions
        } catch (JacksonException e) {
            throw new FailedToParseFormDataException("Invalid JSON format for announcement data: " + jsonAnnouncementModel);
        }
        
        // Validate the announcementModel
        if (announcementFromForm != null) {
            // Validate the announcementModel
            Set<ConstraintViolation<AnnouncementModel>> violations = validator.validate(announcementFromForm);

            // Check if the validation failed
            if (!violations.isEmpty()) {
                // Collect violation messages
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<AnnouncementModel> violation : violations) {
                    sb.append(violation.getPropertyPath().toString())
                        .append(" ")
                        .append(violation.getMessage())
                        .append("; ");
                }
                
                // Throw exception with all violation messages
                throw new FailedToParseFormDataException("Announcement data validation failed: " + sb.toString());
            }
        }

        // Create announcement
        AnnouncementModel createdAnnouncement = announcementService.createAnnouncement(courseId, announcementFromForm, files);

        // Return the created announcement
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAnnouncement.toDTO());
    }

    /**
     * Update announcement
     * 
     * @param announcementId - ID of the announcement to update
     * @param updatedAnnouncement - AnnouncementModel with updated fields
     * @return The updated AnnouncementModel
     * @throws AnnouncementNotFoundException If the announcement with the given ID does not exist
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to update this announcement
     */
    @PutMapping("/{announcementId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<AnnouncementDTO> updateAnnouncement(@PathVariable String announcementId, @Valid @RequestBody AnnouncementModel updatedAnnouncement) {
        // Try to update announcement
        AnnouncementModel updated = announcementService.updateAnnouncement(announcementId, updatedAnnouncement);
        
        // Return the updated announcement with 200 OK
        return ResponseEntity.ok(updated.toDTO());
    }

    /**
     * Update announcement
     * 
     * @param announcementId - ID of the announcement to update
     * @param updatedAnnouncement - AnnouncementModel with updated fields
     * @return The updated AnnouncementModel
     * @throws AnnouncementNotFoundException If the announcement with the given ID does not exist
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to update this announcement
     */
    @PutMapping(path="/{announcementId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<AnnouncementDTO> updateAnnouncement(
        @PathVariable String announcementId, 
        @RequestPart(value="announcement", required=false) String jsonAnnouncementModel,
        @RequestPart(value="files", required=false) List<MultipartFile> files,
        @RequestPart(value="removeFileIds", required=false) List<String> removeFileIds,
        @RequestPart(value="replaceAll", required=false) Boolean replaceAll
    ) throws IOException {
        // Convert json to AnnouncementModel object
        AnnouncementModel updatedAnnouncement;
        try {
            updatedAnnouncement = jsonAnnouncementModel != null
                ? objectMapper.readValue(jsonAnnouncementModel, AnnouncementModel.class)
                : null;

        // Catch JSON parsing exceptions
        } catch (JacksonException e) {
            throw new FailedToParseFormDataException("Invalid JSON format for announcement data: " + jsonAnnouncementModel);
        }

        // Validate the updatedAnnouncement
        if (updatedAnnouncement != null) {
            // Validate the announcementModel
            Set<ConstraintViolation<AnnouncementModel>> violations = validator.validate(updatedAnnouncement);

            // Check if the validation failed
            if (!violations.isEmpty()) {
                // Collect violation messages
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<AnnouncementModel> violation : violations) {
                    sb.append(violation.getPropertyPath().toString())
                        .append(" ")
                        .append(violation.getMessage())
                        .append("; ");
                }

                // Throw exception with all violation messages
                throw new FailedToParseFormDataException("Announcement data validation failed: " + sb.toString());
            }
        }

        // Try to update announcement
        AnnouncementModel updated = announcementService.updateAnnouncement(
            announcementId,
            updatedAnnouncement,
            files,
            removeFileIds,
            replaceAll
        );
        
        // Return the updated announcement with 200 OK
        return ResponseEntity.ok(updated.toDTO());
    }

    /**
     * Patch announcement
     * 
     * @param announcementId - ID of the announcement to patch
     * @param patchedAnnouncement - AnnouncementModel with fields to patch
     * @return The patched AnnouncementModel
     * @throws AnnouncementNotFoundException If the announcement with the given ID does not exist
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to patch this announcement
     */
    @PatchMapping("/patch/{announcementId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<AnnouncementDTO> patchAnnouncement(@PathVariable String announcementId, @RequestBody AnnouncementModel patchedAnnouncement) {
        // Try to patch announcement
        AnnouncementModel patched = announcementService.patchAnnouncement(announcementId, patchedAnnouncement);
        
        // Return the patched announcement with 200 OK
        return ResponseEntity.ok(patched.toDTO());
    }

    /**
     * Patch announcement
     * 
     * @param announcementId - ID of the announcement to patch
     * @param patchedAnnouncement - AnnouncementModel with fields to patch
     * @return The patched AnnouncementModel
     * @throws AnnouncementNotFoundException If the announcement with the given ID does not exist
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to patch this announcement
     */
    @PatchMapping(path="/patch/{announcementId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<AnnouncementDTO> patchAnnouncement(
        @PathVariable String announcementId, 
        @RequestPart(value="announcement", required=false) String jsonAnnouncementModel,
        @RequestPart(value="files", required=false) List<MultipartFile> files,
        @RequestPart(value="removeFileIds", required=false) List<String> removeFileIds,
        @RequestPart(value="replaceAll", required=false) Boolean replaceAll
    ) throws IOException {
        // Convert json to AnnouncementModel object
        AnnouncementModel patchedAnnouncementModel;
        try {
            patchedAnnouncementModel = jsonAnnouncementModel != null
                ? objectMapper.readValue(jsonAnnouncementModel, AnnouncementModel.class)
                : null;

        // Catch JSON parsing exceptions
        } catch (JacksonException e) {
            throw new FailedToParseFormDataException("Invalid JSON format for announcement data: " + jsonAnnouncementModel);
        }

        // Validate the patchedAnnouncementModel
        if (patchedAnnouncementModel != null) {
            // Validate the announcementModel
            Set<ConstraintViolation<AnnouncementModel>> violations = validator.validate(patchedAnnouncementModel);

            // Check if the validation failed
            if (!violations.isEmpty()) {
                // Collect violation messages
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<AnnouncementModel> violation : violations) {
                    sb.append(violation.getPropertyPath().toString())
                        .append(" ")
                        .append(violation.getMessage())
                        .append("; ");
                }

                // Throw exception with all violation messages
                throw new FailedToParseFormDataException("Announcement data validation failed: " + sb.toString());
            }
        }

        // Try to patch announcement
        AnnouncementModel patched = announcementService.patchAnnouncement(
            announcementId,
            patchedAnnouncementModel,
            files,
            removeFileIds,
            replaceAll
        );
        
        // Return the patched announcement with 200 OK
        return ResponseEntity.ok(patched.toDTO());
    }

    /**
     * Delete announcement
     * 
     * @param announcementId - ID of the announcement to delete
     * @return ResponseEntity with no content
     * @throws AnnouncementNotFoundException If the announcement with the given ID does not exist
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to delete this announcement
     */
    @DeleteMapping("/delete/{announcementId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable String announcementId) {
        // Delete the announcement
        announcementService.deleteAnnouncement(announcementId);

        // Return no content status
        return ResponseEntity.noContent().build();
    }
}
