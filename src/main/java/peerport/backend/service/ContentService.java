package peerport.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import peerport.backend.database.ContentRepository;
import peerport.backend.dto.ContentDTO;
import peerport.backend.dto.ContentWithAllDetailsDTO;
import peerport.backend.dto.ContentWithChildrenDTO;
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
    public List<ContentModel> getAllContentIndividual() {
        return contentRepository.findAll();
    }

    // Get structured content
    public List<ContentWithChildrenDTO> getStructuredContent() {
        // Get the content from the repository
        List<ContentModel> contentList = contentRepository.findAll();

        // Convert to ContentWithChildrenDTO
        List<ContentWithChildrenDTO> dtoList = new ArrayList<>();
        for (ContentModel content: contentList) {
            dtoList.add(content.toContentWithChildrenDTO());
        }

        // Check for root content (content without a parent)
        List<ContentWithChildrenDTO> rootContent = new ArrayList<>();

        for (ContentWithChildrenDTO contentDTO: dtoList) {
            if (contentDTO.parentContent == null) {
                rootContent.add(contentDTO);
            }
        }

        // Return the structured content
        return rootContent;
    }

    // Get content by ID
    public Optional<ContentWithAllDetailsDTO> getContentById(String contentId) {
        return contentRepository.findById(contentId).map(ContentModel::toContentWithAllDetailsDTO);
    }

    // Update content
    public Optional<ContentDTO> updateContent(String contentId, ContentModel updatedContent) throws IllegalArgumentException {
        // Get the content from the repository
        Optional<ContentModel> existingContent = contentRepository.findById(contentId);

        // Check if id was valid
        if (existingContent.isEmpty()) {
            throw new IllegalArgumentException("Content with ID " + contentId + " does not exist.");
        }

        // update fields
        ContentModel content = existingContent.get();
        content.setTitle(updatedContent.getTitle());
        content.setDescription(updatedContent.getDescription());
        content.setVisible(updatedContent.getVisible());

        // Save the updated content
        contentRepository.save(content);
        return Optional.of(content.toDTO());
    }

    // Patch / partial update course
    public Optional<ContentDTO> patchContent(String contentId, ContentDTO patchedContent) throws IllegalArgumentException {
        // Get the content from the repository
        Optional<ContentModel> existingContent = contentRepository.findById(contentId);

        // Check if id was valid
        if (existingContent.isEmpty()) {
            throw new IllegalArgumentException("Content with ID " + contentId + " does not exist.");
        }

        // patch fields
        ContentModel content = existingContent.get();
        if (patchedContent.title != null) {
            content.setTitle(patchedContent.title);
        }
        if (patchedContent.description != null) {
            content.setDescription(patchedContent.description);
        }
        if (patchedContent.visible != null) {
            content.setVisible(patchedContent.visible);
        }

        // Save the patched content
        contentRepository.save(content);
        return Optional.of(content.toDTO());
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
