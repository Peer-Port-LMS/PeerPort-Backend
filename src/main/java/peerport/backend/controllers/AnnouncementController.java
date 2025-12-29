package peerport.backend.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
        List<AnnouncementDTO> announcementDTOs = announcements.stream().map(AnnouncementModel::toDTO).toList();
        return ResponseEntity.ok(announcementDTOs);
    }

    // Get announcement by ID
    @GetMapping("/{announcementId}")
    public ResponseEntity<AnnouncementDTO> getAnnouncementById(@PathVariable String announcementId) {
        // Get the announcement by ID
        Optional<AnnouncementModel> announcement = announcementService.getAnnouncementById(announcementId);

        // Check if announcement is empty and return 404 if so
        if (announcement.isEmpty()) {
            return ResponseEntity.notFound().build();
        
        // Otherwise, return the announcement with 200 OK
        } else {
            AnnouncementDTO ann = announcement.get().toDTO();
            return ResponseEntity.ok(ann);
        }
    }

    // Create announcement
    @PostMapping("/{courseId}")
    public ResponseEntity<AnnouncementDTO> createAnnouncement(@PathVariable String courseId, @Valid @RequestBody AnnouncementModel announcementModel) {
        AnnouncementModel createdAnnouncement;

        // Try to create announcement and catch illegal argument exception
        try {
            createdAnnouncement = announcementService.createAnnouncement(courseId, announcementModel);
        
        // Catch illegal argument exception and return 404 Not Found
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }

        // Return the created announcement with 201 Created
        return ResponseEntity.status(201).body(createdAnnouncement.toDTO());
    }

    // Update announcement
    @PutMapping("/{announcementId}")
    public ResponseEntity<AnnouncementDTO> updateAnnouncement(@PathVariable String announcementId, @RequestBody AnnouncementModel updatedAnnouncement) {
        AnnouncementModel updated;
        
        // Try to update announcement
        try {
            updated = announcementService.updateAnnouncement(announcementId, updatedAnnouncement);

        // Catch illegal argument exception and return 404 Not Found
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        
        // Return the updated announcement with 200 OK
        return ResponseEntity.ok(updated.toDTO());
    }

    // Delete announcement
    @DeleteMapping("/delete/{announcementId}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable String announcementId) {
        // Delete the announcement
        boolean deleted = announcementService.deleteAnnouncement(announcementId);
        
        // Return appropriate response based on deletion result
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
