package peerport.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.ContentRepository;
import peerport.backend.model.ContentModel;

@Service
public class ContentService {
    
    @Autowired
    private ContentRepository contentRepository;

    // Create content
    public ContentModel createContent(ContentModel content) {
        return contentRepository.save(content);
    }

    // Get all content
    public List<ContentModel> getAllContent() {
        return contentRepository.findAll();
    }

    // Get content by ID
    public Optional<ContentModel> getContentById(String contentId) {
        return contentRepository.findById(contentId);
    }

    // Update content
    public Optional<ContentModel> updateContent(String contentId, ContentModel updatedContent) {
        return contentRepository.findById(contentId).map(content -> {
            content.setTitle(updatedContent.getTitle());
            content.setDescription(updatedContent.getDescription());
            content.setVisible(updatedContent.getVisible());
            content.setDateCreated(updatedContent.getDateCreated());
            content.setDateUpdated(updatedContent.getDateUpdated());
            content.setCourse(updatedContent.getCourse());
            content.setParentContent(updatedContent.getParentContent());
            return contentRepository.save(content);
        });
    }

    // Delete content
    public boolean deleteContent(String contentId) {
        if (contentRepository.existsById(contentId)) {
            contentRepository.deleteById(contentId);
            return true;
        }
        return false;
    }
}
