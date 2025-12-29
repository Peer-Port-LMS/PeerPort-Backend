package peerport.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.AnnouncementsRepository;
import peerport.backend.model.AnnouncementModel;

@Service
public class AnnouncementService {
    
    @Autowired
    private AnnouncementsRepository announcementsRepository;

    @Autowired
    private CourseService courseService;

    // Create Announcment
    public AnnouncementModel createAnnouncement(String courseId, AnnouncementModel announcement) throws IllegalArgumentException {
        // Get the course by ID
        var courseOpt = courseService.getCourseById(courseId);
        if (courseOpt.isEmpty()) {
            throw new IllegalArgumentException("Course with ID " + courseId + " not found.");
        }

        // Set the course to the announcement
        announcement.setCourse(courseOpt.get());

        // Save the announcement
        return announcementsRepository.save(announcement);
    }

    // Get all announcements
    public List<AnnouncementModel> getAllAnnouncements() {
        return announcementsRepository.findAll();
    }

    // Get announcement by ID
    public Optional<AnnouncementModel> getAnnouncementById(String announcementId) {
        return announcementsRepository.findById(announcementId);
    }

    // Update announcement
    public Optional<AnnouncementModel> updateAnnouncement(String announcementId, AnnouncementModel updatedAnnouncement) {
        return announcementsRepository.findById(announcementId).map(announcement -> {
            announcement.setTitle(updatedAnnouncement.getTitle());
            announcement.setContent(updatedAnnouncement.getContent());
            announcement.setDateCreated(updatedAnnouncement.getDateCreated());
            announcement.setDateUpdated(updatedAnnouncement.getDateUpdated());
            announcement.setCourse(updatedAnnouncement.getCourse());
            return announcementsRepository.save(announcement);
        });
    }

    // Delete announcement
    public boolean deleteAnnouncement(String announcementId) {
        if (announcementsRepository.existsById(announcementId)) {
            announcementsRepository.deleteById(announcementId);
            return true;
        }
        return false;
    }
}
