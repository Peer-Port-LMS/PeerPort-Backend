package peerport.backend.service;

import java.io.FileNotFoundException;
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
import peerport.backend.exceptions.files.InvalidFileTypeException;
import peerport.backend.model.FileModel;

/**
 * Service for handling file and FileModel operations
 */
@Service
public class FileService {

    @Autowired
    private FilesRepository filesRepository;
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    private String coursesDir = "courses";

    // Regular functions
    public Optional<FileModel> getFileById(String fileId) {
        return filesRepository.findById(fileId);
    }


    // Specific functions
    /**
     * Saves a course image
     * @param file - The image file to save
     * @param courseId - The ID of the course
     * @return The new file model
     * @throws IOException If there was an error saving the file
     * @throws InvalidFileTypeException If the file type is invalid (Gets handled in GlobalExceptionHandler)
     */
    public FileModel saveCourseImage(MultipartFile file, String courseId) throws IOException {
        // Get the file extension
        String fileExtension = getFileExtension(file.getOriginalFilename());

        // Create the new file name
        String fileName = "course_" + courseId + (fileExtension.isEmpty() ? "" : "." + fileExtension);

        // Get the content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidFileTypeException("File must be an image. Received: " + contentType);
        }

        // Save the image
        String savedImagePath = saveFile(file, this.uploadDir + "/" + this.coursesDir + "/" + fileName);

        // Create the new FileModel
        FileModel newFile = new FileModel(fileName, savedImagePath, fileExtension, contentType);

        // Save the file model to the database
        return filesRepository.save(newFile);
    }

    // Delete a file
    /**
     * Deletes a file
     * @param file - The file model which links to a file to delete
     * @throws IOException If there was an error deleting the file
     * @throws FileNotFoundException If the file was not found on the filesystem
     */
    public void deleteFile(FileModel file) throws IOException, FileNotFoundException {
        // Delete the file from the filesystem
        if (!deleteFileHelper(file.getFilePath())) {
            throw new FileNotFoundException("File not found on the filesystem: " + file.getFilePath());
        }

        // Delete the file from the repo first
        filesRepository.delete(file);
    }


    /**
     * Gets the file extension from a file name
     * @param fileName - The file name
     * @return The file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    /**
     * Saves a file to the specified path
     * @param file - The file to save
     * @param path - The path to save the file to
     * @return The path where the file was saved
     * @throws IOException If there was an error saving the file
     */
    private String saveFile(MultipartFile file, String path) throws IOException {
        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(path).getParent();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save the file
        Path filePath = Paths.get(path);
        Files.write(filePath, file.getBytes());

        return filePath.toString();
    }


    /**
     * Deletes a file at the specified path
     * @param path - The path of the file to delete
     * @return True if the file was deleted, false if it did not exist
     * @throws IOException If there was an error deleting the file
     */
    private boolean deleteFileHelper(String path) throws IOException{
        Path filePath = Paths.get(path);
        return Files.deleteIfExists(filePath);
    }
}
