package peerport.backend.exceptions.content;

public class ContentNotFoundException extends RuntimeException {
    public ContentNotFoundException(String contentId) {
        super("Content with ID " + contentId + " not found.");
    }
}
