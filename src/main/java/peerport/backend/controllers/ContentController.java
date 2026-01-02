package peerport.backend.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import peerport.backend.dto.ContentDTO;
import peerport.backend.dto.ContentWithAllDetailsDTO;
import peerport.backend.dto.ContentWithChildrenDTO;
import peerport.backend.model.ContentModel;
import peerport.backend.service.ContentService;

@RestController
@RequestMapping("/content")
public class ContentController {
    
    @Autowired
    private ContentService contentService;

    @GetMapping
    public ResponseEntity<List<ContentWithChildrenDTO>> getAllContent() {
        List<ContentWithChildrenDTO> contentList = contentService.getStructuredContent();
        return ResponseEntity.ok(contentList);
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentWithAllDetailsDTO> getContentById(@PathVariable String contentId) {
        try {
            // Get the content
            Optional<ContentWithAllDetailsDTO> contentOpt = contentService.getContentById(contentId);

            // Check if we found the content
            if (contentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Return the content if found
            return ResponseEntity.ok(contentOpt.get());

        // Return not found if invalid ID
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ContentModel> createContent(@RequestBody ContentModel content) {
        ContentModel savedContent = contentService.createContent(content);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedContent);
    }

    @PutMapping("/{contentId}")
    public ResponseEntity<ContentDTO> updateContent(@PathVariable String contentId, @RequestBody ContentModel content) {
        return contentService.updateContent(contentId, content)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{contentId}")
    public ResponseEntity<ContentDTO> partiallyUpdateContent(@PathVariable String contentId, @RequestBody ContentModel content) {
        return contentService.updateContent(contentId, content)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable String contentId) {
        boolean deleted = contentService.deleteContent(contentId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
