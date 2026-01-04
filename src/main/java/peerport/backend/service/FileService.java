package peerport.backend.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import peerport.backend.database.FilesRepository;
import peerport.backend.model.ContentModel;
import peerport.backend.exceptions.files.InvalidFileTypeException;
import peerport.backend.model.AnnouncementModel;
import peerport.backend.model.AssignmentModel;
import peerport.backend.model.AssignmentSubmissionModel;
import peerport.backend.model.FileModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;

/**
 * Service for handling file and FileModel operations
 */
@Service
public class FileService {

    @Autowired
    private FilesRepository filesRepository;

    @Autowired
    private AuthService authService;

    
    @Value("${file.upload-dir}")
    private String uploadDir;
    private String coursesDir = "courses";

    // Regular functions
    public FileModel getFileById(String fileId) throws FileNotFoundException {
        // Get the file by ID
        Optional<FileModel> fileOpt = filesRepository.findById(fileId);

        // Check if the file exists
        if (fileOpt.isEmpty()) {
            throw new FileNotFoundException(fileId);
        }

        // Get the file
        FileModel file = fileOpt.get();

        // Get the current user and their role
        UserModel currentUser = authService.getCurrentUser();
        Role userRole = currentUser.getRole();

        // If admin allow access
        if (userRole == Role.ADMIN) return file;

        // Check if user is allowed to access the file
        if (file.getCourse() != null) {
            // Check if the user is enrolled in the course
            if (file.getCourse().getUsers().contains(currentUser)) {
                return file;
            }
        } else if (file.getContent() != null) {
            // Check if the user is allowed to access the content
            if (file.getContent().getCourse().getUsers().contains(currentUser)) {
                return file;
            }
        } else if (file.getAnnouncement() != null) {
            // Check if the user is allowed to access the announcement
            if (file.getAnnouncement().getCourse().getUsers().contains(currentUser)) {
                return file;
            }
        } else if (file.getAssignmentSubmission() != null) {
            // Check if the user is allowed to access the assignment submission
            if (file.getAssignmentSubmission().getUser().getUserId().equals(currentUser.getUserId())) {
                return file;
            }
        }

        // File is not associated with any entity
        throw new FileNotFoundException("File with ID: " + fileId + " is not associated with any entity.");
    }

    /**
     * Saves files attached to content
     * @param files - The list of files to save
     * @param content - The content the files belong to
     * @param courseId - The ID of the course
     * @return The list of saved FileModels
     * @throws IOException If there was an error saving the files
     */
    @Transactional
    public List<FileModel> saveContentFiles(List<MultipartFile> files, ContentModel content, String courseId) throws IOException {
        List<FileModel> savedFiles = new ArrayList<>();

        long baseTimestamp = System.currentTimeMillis();
        int fileCounter = 0;
        
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = String.format("content_%s_%d_%d%s",
                content.getContentId(),
                baseTimestamp,
                fileCounter,
                fileExtension.isEmpty() ? "" : "." + fileExtension
            );

            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String savedFilePath = saveFile(
                file,
                this.uploadDir + "/" + this.coursesDir + "/" + courseId + "/content/" + content.getContentId() + "/" + fileName
            );

            FileModel newFile = new FileModel(fileName, savedFilePath, fileExtension, contentType);
            newFile.setContent(content);

            FileModel savedFile = filesRepository.save(newFile);
            savedFiles.add(savedFile);
            fileCounter++;
        }

        return savedFiles;
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
    @Transactional
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
        String savedImagePath = saveFile(file, this.uploadDir + "/" + this.coursesDir + "/" + courseId + "/" + fileName);

        // Create the new FileModel
        FileModel newFile = new FileModel(fileName, savedImagePath, fileExtension, contentType);

        // Save the file model to the database
        return filesRepository.save(newFile);
    }

    /**
     * Saves files attached to an announcement
     * 
     * @param files - The list of files to save
     * @param announcement - The announcement the files belong to
     * @param courseId - The ID of the course
     * @return The list of saved FileModels
     * @throws IOException If there was an error saving the files
     */
    @Transactional
    public List<FileModel> saveAnnouncementFiles(List<MultipartFile> files, AnnouncementModel announcement, String courseId) throws IOException {
        List<FileModel> savedFiles = new ArrayList<>();

        long baseTimestamp = System.currentTimeMillis();
        int fileCounter = 0;
        
        for (MultipartFile file : files) {
            // Skip empty files
            if (file == null || file.isEmpty()) {
                continue;
            }

            // Get the file extension
            String fileExtension = getFileExtension(file.getOriginalFilename());

            // Create the new file name
            String fileName = String.format("announcement_%s_%d_%d%s",
                announcement.getAnnouncementId(),
                baseTimestamp,
                fileCounter,
                fileExtension.isEmpty() ? "" : "." + fileExtension
            );

            // Get the content type
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream"; // Default binary type
            }

            // Save the file
            String savedFilePath = saveFile(
                file, 
                this.uploadDir + "/" + this.coursesDir + "/" + courseId + "/announcements/" + announcement.getAnnouncementId() + "/" + fileName
            );

            // Create the new FileModel
            FileModel newFile = new FileModel(fileName, savedFilePath, fileExtension, contentType);

            // Link back to parent announcement for ORM mapping
            newFile.setAnnouncement(announcement);

            // Save the file model to the database
            FileModel savedFile = filesRepository.save(newFile);
            savedFiles.add(savedFile);
            
            // Increment counter
            fileCounter++;
        }

        // Return the saved files
        return savedFiles;
    }

    /**
     * Saves assignment files
     * 
     * @param files - List of files to save
     * @param assignment - AssignmentModel to associate files with
     * @param courseId - ID of the course
     * @return List of saved FileModels
     * @throws IOException If there was an error saving the files
     */
    @Transactional
    public List<FileModel> saveAssignmentFiles(List<MultipartFile> files, AssignmentModel assignment, String courseId) throws IOException {
        List<FileModel> savedFiles = new ArrayList<>();
        
        long baseTimestamp = System.currentTimeMillis();
        int fileCounter = 0;
        
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = String.format("%d_%d%s",
                baseTimestamp,
                fileCounter,
                fileExtension.isEmpty() ? "" : "." + fileExtension
            );
            
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            String savedFilePath = saveFile(
                file,
                this.uploadDir + "/" + this.coursesDir + "/" + courseId + "/assignments/" + assignment.getAssignmentId() + "/" + fileName
            );
            
            FileModel newFile = new FileModel(fileName, savedFilePath, fileExtension, contentType);
            newFile.setAssignment(assignment);
            
            FileModel savedFile = filesRepository.save(newFile);
            savedFiles.add(savedFile);
            
            fileCounter++;
        }
        
        return savedFiles;
    }

    /**
     * Saves files attached to an assignment submission
     * 
     * @param files - The list of files to save
     * @param assignment - The assignment the submission belongs to
     * @param assignmentSubmission - The assignment submission the files belong to
     * @return The list of saved FileModels
     * @throws IOException If there was an error saving the files
     */
    @Transactional
    public List<FileModel> saveAssignmentSubmissionFiles(
        List<MultipartFile> files, 
        AssignmentModel assignment,
        AssignmentSubmissionModel assignmentSubmission
    ) throws IOException {
        List<FileModel> savedFiles = new ArrayList<>();
        
        long baseTimestamp = System.currentTimeMillis();
        int fileCounter = 0;
        
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            
            // Get the file extension
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = String.format("%d_%d%s",
                baseTimestamp,
                fileCounter,
                fileExtension.isEmpty() ? "" : "." + fileExtension
            );
            
            // Get the content type
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // Save the file
            String savedFilePath = saveFile(
                file,
                this.uploadDir + "/" 
                + this.coursesDir + "/" 
                + assignment.getCourse().getCourseId() + "/assignments/" 
                + assignment.getAssignmentId() + "/submissions/" + fileName
            );
            
            // Make a new file model
            FileModel newFile = new FileModel(fileName, savedFilePath, fileExtension, contentType);
            newFile.setAssignmentSubmission(assignmentSubmission);
            
            // Save the file model to the database
            FileModel savedFile = filesRepository.save(newFile);
            savedFiles.add(savedFile);
            
            // Increment counter
            fileCounter++;
        }
        
        // Return the saved files
        return savedFiles;
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
