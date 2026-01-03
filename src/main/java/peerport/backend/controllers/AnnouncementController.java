package peerport.backend.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import peerport.backend.dto.AnnouncementDTO;
import peerport.backend.model.AnnouncementModel;
import peerport.backend.service.AnnouncementService;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
    
    @Autowired
    private AnnouncementService announcementService;

    // Get all announcements
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
     */
    @PostMapping("/{courseId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<AnnouncementDTO> createAnnouncement(@PathVariable String courseId, @Valid @RequestBody AnnouncementModel announcementModel) {
        // Create announcement
        AnnouncementModel createdAnnouncement = announcementService.createAnnouncement(courseId, announcementModel);

        // Return the created announcement
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAnnouncement.toDTO());
    }

    /**
     * Update announcement
     * 
     * @param announcementId - ID of the announcement to update
     * @param updatedAnnouncement - AnnouncementModel with updated fields
     * @return The updated AnnouncementModel
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
     * Patch announcement
     * 
     * @param announcementId - ID of the announcement to patch
     * @param patchedAnnouncement - AnnouncementModel with fields to patch
     * @return The patched AnnouncementModel
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
     * Delete announcement
     * 
     * @param announcementId - ID of the announcement to delete
     * @return ResponseEntity with no content
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
