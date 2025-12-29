package peerport.backend.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import peerport.backend.model.AnnouncementModel;
import peerport.backend.service.AnnouncementService;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
    
    @Autowired
    private AnnouncementService announcementService;

    // Get all announcements
    @GetMapping
    public ResponseEntity<List<AnnouncementModel>> getAllAnnouncements() {
        List<AnnouncementModel> announcements = announcementService.getAllAnnouncements();
        return ResponseEntity.ok(announcements);
    }

    // Get announcement by ID
    @GetMapping("/{announcementId}")
    public ResponseEntity<AnnouncementModel> getAnnouncementById(@PathVariable String announcementId) {
        // Get the announcement by ID
        Optional<AnnouncementModel> announcement = announcementService.getAnnouncementById(announcementId);

        // Check if announcement is empty and return 404 if so
        if (announcement.isEmpty()) {
            return ResponseEntity.notFound().build();
        
        // Otherwise, return the announcement with 200 OK
        } else {
            AnnouncementModel ann = announcement.get();
            return ResponseEntity.ok(ann);
        }
    }

    // Create announcement
    @PostMapping
    public ResponseEntity<AnnouncementModel> createAnnouncement(@RequestBody AnnouncementModel announcementModel) {
        AnnouncementModel createdAnnouncement = announcementService.createAnnouncement(announcementModel);
        return ResponseEntity.status(201).body(createdAnnouncement);
    }

    // Update announcement
    @PostMapping("/{announcementId}")
    public ResponseEntity<AnnouncementModel> updateAnnouncement(@PathVariable String announcementId, @RequestBody AnnouncementModel updatedAnnouncement) {
        // Update the announcement
        Optional<AnnouncementModel> updated = announcementService.updateAnnouncement(announcementId, updatedAnnouncement);
        
        // Check if the update was successful and return appropriate response
        if (updated.isPresent()) {
            return ResponseEntity.ok(updated.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete announcement
    @PostMapping("/delete/{announcementId}")
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
