package peerport.backend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import peerport.backend.model.ContentModel;
import peerport.backend.service.ContentService;

@RestController
@RequestMapping("/content")
public class ContentController {
    
    @Autowired
    private ContentService contentService;

    @GetMapping
    public ResponseEntity<List<ContentModel>> getAllContent() {
        List<ContentModel> contentList = contentService.getAllContent();
        return ResponseEntity.ok(contentList);
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentModel> getContentById(@PathVariable String contentId) {
        return contentService.getContentById(contentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ContentModel> createContent(@RequestBody ContentModel content) {
        ContentModel savedContent = contentService.createContent(content);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedContent);
    }

    @PutMapping("/{contentId}")
    public ResponseEntity<ContentModel> updateContent(@PathVariable String contentId, @RequestBody ContentModel content) {
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
