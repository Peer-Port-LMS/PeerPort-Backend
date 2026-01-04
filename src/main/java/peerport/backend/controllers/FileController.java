package peerport.backend.controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    public ResponseEntity<Resource> getFileById(@PathVariable String fileId) throws FileNotFoundException, MalformedURLException, IOException {
        // Get the file from the database and perform authorization checks
        FileModel fileModel = fileService.getFileById(fileId);

        // Load the file as a Resource
        Path filePath = Paths.get(fileModel.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        // Check if the resource exists and is readable
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type
        String contentType = fileModel.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        }

        // Return the file as a response entity
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileModel.getFileName() + "\"")
                .body(resource);
    }
}
