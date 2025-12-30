package peerport.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import peerport.backend.database.FilesRepository;
import peerport.backend.model.FileModel;

@Service
public class FileService {

    @Autowired
    private FilesRepository filesRepository;
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    private String coursesDir = "courses";

    @Value("${server.hosting-url}")
    private String serverUrl;
    private static String filesEndpoint = "files";

    // Regular functions
    public Optional<FileModel> getFileById(String fileId) {
        return filesRepository.findById(fileId);
    }


    // Specific functions
    public String getFileUrl(String fileId) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            System.err.println("Warning: serverUrl is not set properly. Default to localhost");
            return "http://localhost:8080/" + filesEndpoint + "/" + fileId;
        }
        return serverUrl + "/" + filesEndpoint + "/" + fileId;
    }

    // Save a course image
    public FileModel saveCourseImage(MultipartFile file, String courseId) throws IOException {
        // Get the file extension
        String fileExtension = getFileExtension(file.getOriginalFilename());

        // Create the new file name
        String fileName = "course_" + courseId + (fileExtension.isEmpty() ? "" : "." + fileExtension);

        // Save the image
        String savedImagePath = saveFile(file, this.uploadDir + "/" + this.coursesDir + "/" + fileName);

        // Get the content type
        String contentType = file.getContentType();

        // Save the FileModel
        FileModel newFile = new FileModel(fileName, savedImagePath, fileExtension, contentType);
        filesRepository.save(newFile);

        // Get the url
        newFile.setUrl(getFileUrl(newFile.getFileId()));

        // Save the file model to the database
        return filesRepository.save(newFile);
    }

    // Delete a file
    public void deleteFile(FileModel file) throws IOException, IllegalArgumentException {
        // Delete the file from the repo first
        filesRepository.delete(file);

        // Delete the file from the filesystem
        if (!deleteFile(file.getFilePath())) {
            throw new IllegalArgumentException("File not found on the filesystem: " + file.getFilePath());
        }
    }

    // Get file extension
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    // Saves a file
    private String saveFile(MultipartFile file, String path) throws IOException {
        // Create directory if it doesn't exsit
        Path uploadPath = Paths.get(path).getParent();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save the file
        Path filePath = Paths.get(path);
        Files.write(filePath, file.getBytes());

        return filePath.toString();
    }

    // Delete a file
    private boolean deleteFile(String path) throws IOException{
        Path filePath = Paths.get(path);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            return true;
        }
        return false;
    }
}
