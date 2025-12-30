package peerport.backend.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;

import peerport.backend.model.FileModel;
import peerport.backend.service.FileService;

@RestController
@RequestMapping("/files")
public class FileController {
    
    @Autowired
    private FileService fileService;
    
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> getFileById(@PathVariable String fileId) {
        try {
            // Get the file from the database
            Optional<FileModel> fileModel = fileService.getFileById(fileId);

            // Check if the file exists
            if (!fileModel.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            // Auth and permission checks here

            // Load the file as a Resource
            Path filePath = Paths.get(fileModel.get().getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            // Check if the resource exists and is readable
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = fileModel.get().getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }

            // Return the file as a response entity
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileModel.get().getFileName() + "\"")
                    .body(resource);
        
        // Catch IOExceptions
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
