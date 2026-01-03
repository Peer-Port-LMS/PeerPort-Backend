package peerport.backend.exceptions.announcements;

public class AnnouncementNotFoundException extends RuntimeException {
    public AnnouncementNotFoundException(String announcementId) {
        super("Announcement with ID " + announcementId + " not found");
    }
}
