package peerport.backend.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import peerport.backend.controllers.FileController;
import peerport.backend.model.FileModel;
import peerport.backend.service.FileService;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileController Unit Tests")
class FileControllerTest {

    @InjectMocks
    private FileController fileController;

    @Mock
    private FileService fileService;

    @TempDir
    Path tempDir;

    @Test
    void getFileById_whenResourceExists_returns200() throws Exception {
        Path filePath = tempDir.resolve("test.txt");
        Files.writeString(filePath, "hello");

        FileModel fileModel = new FileModel("test.txt", filePath.toString(), "txt", "text/plain");
        when(fileService.getFileById("f1")).thenReturn(fileModel);

        ResponseEntity<Resource> response = fileController.getFileById("f1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("inline; filename=\"test.txt\"", response.getHeaders().getFirst("Content-Disposition"));
    }

    @Test
    void getFileById_whenResourceMissing_returns404() throws Exception {
        Path missingPath = tempDir.resolve("missing.txt");
        FileModel fileModel = new FileModel("missing.txt", missingPath.toString(), "txt", "text/plain");
        when(fileService.getFileById("f1")).thenReturn(fileModel);

        ResponseEntity<Resource> response = fileController.getFileById("f1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getFileById_whenServiceThrows_propagatesException() throws Exception {
        when(fileService.getFileById("missing")).thenThrow(new FileNotFoundException("missing"));

        assertThrows(FileNotFoundException.class, () -> fileController.getFileById("missing"));
    }

    @Test
    void getFileById_whenContentTypeBlank_fallsBackAndStillReturns200() throws Exception {
        Path filePath = tempDir.resolve("test.bin");
        Files.write(filePath, new byte[] {1, 2, 3});

        FileModel fileModel = new FileModel("test.bin", filePath.toString(), "bin", "");
        when(fileService.getFileById("f2")).thenReturn(fileModel);

        ResponseEntity<Resource> response = fileController.getFileById("f2");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
