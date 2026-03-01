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
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.files.InvalidFileTypeException;
import peerport.backend.model.AnnouncementModel;
import peerport.backend.model.AssignmentModel;
import peerport.backend.model.AssignmentSubmissionModel;
import peerport.backend.model.FileModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Service for handling file and FileModel operations
 */
@Service
public class FileService {
    protected static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private FilesRepository filesRepository;

    @Autowired
    private AuthService authService;

    
    @Value("${file.upload-dir}")
    private String uploadDir;
    private String coursesDir = "courses";

    @Value("${file.upload-size-limit}")
    private long fileUploadSizeLimit;

    // Regular functions
    public FileModel getFileById(String fileId) throws FileNotFoundException {
        logger.debug("Attempting to retrieve file with ID: {}", fileId);

        // Get the file by ID
        Optional<FileModel> fileOpt = filesRepository.findById(fileId);

        // Check if the file exists
        if (fileOpt.isEmpty()) {
            logger.warn("File with ID: {} not found", fileId);
            throw new FileNotFoundException(fileId);
        }

        // Get the file
        FileModel file = fileOpt.get();

        // Get the current user and their role
        UserModel currentUser = authService.getCurrentUser();
        Role userRole = currentUser.getRole();

        // If admin allow access
        if (userRole == Role.ADMIN) {
            logger.debug("User with ID: {} is an admin, granting access to file with ID: {}", currentUser.getUserId(), fileId);
            return file;
        }

        // Check if user is allowed to access the file
        if (file.getCourse() != null) {
            // Check if the user is enrolled in the course
            if (file.getCourse().getUsers().contains(currentUser)) {
                logger.debug("User with ID: {} is enrolled in course with ID: {}, granting access to file with ID: {}", currentUser.getUserId(), file.getCourse().getCourseId(), fileId);
                return file;
            }
        } else if (file.getContent() != null) {
            // Check if the user is allowed to access the content
            if (file.getContent().getCourse().getUsers().contains(currentUser)) {
                logger.debug("User with ID: {} is allowed to access content with ID: {}, granting access to file with ID: {}", currentUser.getUserId(), file.getContent().getContentId(), fileId);
                return file;
            }
        } else if (file.getAnnouncement() != null) {
            // Check if the user is allowed to access the announcement
            if (file.getAnnouncement().getCourse().getUsers().contains(currentUser)) {
                logger.debug("User with ID: {} is allowed to access announcement with ID: {}, granting access to file with ID: {}", currentUser.getUserId(), file.getAnnouncement().getAnnouncementId(), fileId);
                return file;
            }
        } else if (file.getAssignmentSubmission() != null) {
            // Check if the user is an instructor of the course
            if (userRole == Role.INSTRUCTOR && file.getAssignmentSubmission().getAssignment().getCourse().getInstructors().contains(currentUser)) {
                logger.debug("User with ID: {} is an instructor for course with ID: {}, granting access to file with ID: {}", currentUser.getUserId(), file.getAssignmentSubmission().getAssignment().getCourse().getCourseId(), fileId);
                return file;
            }

            // Check if the user is allowed to access the assignment submission
            if (file.getAssignmentSubmission().getUser().getUserId().equals(currentUser.getUserId())) {
                logger.debug("User with ID: {} is the owner of assignment submission with ID: {}, granting access to file with ID: {}", currentUser.getUserId(), file.getAssignmentSubmission().getAssignmentSubmissionId(), fileId);
                return file;
            }
        }

        // File is not associated with any entity
        logger.warn("File with ID: {} is not associated with any entity or user does not have access", fileId);
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
        logger.debug("Saving {} files for content with ID: {}", files.size(), content.getContentId());

        List<FileModel> savedFiles = new ArrayList<>();

        long baseTimestamp = System.currentTimeMillis();
        int fileCounter = 0;
        
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                logger.trace("Skipping empty file for content with ID: {}", content.getContentId());
                continue;
            }

            // Get the file extension
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = String.format("content_%s_%d_%d%s",
                content.getContentId(),
                baseTimestamp,
                fileCounter,
                fileExtension.isEmpty() ? "" : "." + fileExtension
            );

            // Get the content type
            String contentType = file.getContentType();
            if (contentType == null) {
                logger.warn("Content type is null for file: {}, defaulting to application/octet-stream", file.getOriginalFilename());
                contentType = "application/octet-stream";
            }

            // Save the file
            String savedFilePath = saveFile(
                file,
                this.uploadDir + "/" + this.coursesDir + "/" + courseId + "/content/" + content.getContentId() + "/" + fileName
            );

            // Create the new FileModel
            FileModel newFile = new FileModel(fileName, savedFilePath, fileExtension, contentType);
            newFile.setContent(content);

            // Save the file model to the database
            FileModel savedFile = filesRepository.save(newFile);
            savedFiles.add(savedFile);
            fileCounter++;
        }

        // Return the saved files
        logger.debug("Successfully saved {} files for content with ID: {}", savedFiles.size(), content.getContentId());
        return savedFiles;
    }


    /**
     * Checks if any of the files exceed the file size limit and throws an exception if they do
     * @param files - The list of files to check
     * @throws FileSizeLimitExceededException If any of the files exceed the file size limit
     */
    public void checkFileSizes(List<MultipartFile> files) throws FileSizeLimitExceededException {
        logger.debug("Checking file sizes for {} files against limit of {} bytes", files != null ? files.size() : 0, fileUploadSizeLimit);
        
        // If files is null, skip the check
        if (files != null) {
            // Check each file size against the limit
            for (MultipartFile file : files) {
                checkFileSize(file);
            }
        }
    }

    /**
     * Checks if a file exceeds the file size limit and throws an exception if it does
     * @param file - The file to check
     * @throws FileSizeLimitExceededException If the file exceeds the file size limit
     */
    public void checkFileSize(MultipartFile file) throws FileSizeLimitExceededException {
        logger.trace("Checking file size for file: {}. Size: {} bytes, Limit: {} bytes", file.getOriginalFilename(), file.getSize(), fileUploadSizeLimit);
        if (file != null && file.getSize() > fileUploadSizeLimit) { // 5MB limit
            logger.warn("File size exceeds limit: {} bytes. File name: {}", file.getSize(), file.getOriginalFilename());
            throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes. File name: " + file.getOriginalFilename());
        }
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
        logger.debug("Saving course image for course with ID: {}", courseId);

        // Get the file extension
        String fileExtension = getFileExtension(file.getOriginalFilename());

        // Create the new file name
        String fileName = "course_" + courseId + (fileExtension.isEmpty() ? "" : "." + fileExtension);

        // Get the content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            logger.warn("Invalid file type for course image: {}. File name: {}", contentType, file.getOriginalFilename());
            throw new InvalidFileTypeException("File must be an image. Received: " + contentType);
        }

        // Save the image
        logger.trace("Saving course image for course with ID: {}. Original file name: {}, New file name: {}, Content type: {}", courseId, file.getOriginalFilename(), fileName, contentType);
        String savedImagePath = saveFile(file, this.uploadDir + "/" + this.coursesDir + "/" + courseId + "/" + fileName);

        // Create the new FileModel
        FileModel newFile = new FileModel(fileName, savedImagePath, fileExtension, contentType);

        // Save the file model to the database
        logger.debug("Successfully saved course image for course with ID: {}. File name: {}", courseId, fileName);
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
        logger.debug("Saving {} files for announcement with ID: {}", files.size(), announcement.getAnnouncementId());

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
            logger.trace("Saving file for announcement with ID: {}. Original file name: {}, New file name: {}, Content type: {}", announcement.getAnnouncementId(), file.getOriginalFilename(), fileName, contentType);
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
        logger.debug("Successfully saved {} files for announcement with ID: {}", savedFiles.size(), announcement.getAnnouncementId());
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
        logger.debug("Saving {} files for assignment with ID: {}", files.size(), assignment.getAssignmentId());
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
            
            logger.trace("Saving file for assignment with ID: {}. Original file name: {}, New file name: {}, Content type: {}", assignment.getAssignmentId(), file.getOriginalFilename(), fileName, contentType);
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
        
        logger.debug("Successfully saved {} files for assignment with ID: {}", savedFiles.size(), assignment.getAssignmentId());
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
        logger.debug("Saving {} files for assignment submission with ID: {}", files.size(), assignmentSubmission.getAssignmentSubmissionId());

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
            logger.trace("Saving file for assignment submission with ID: {}. Original file name: {}, New file name: {}, Content type: {}", assignmentSubmission.getAssignmentSubmissionId(), file.getOriginalFilename(), fileName, contentType);
            String savedFilePath = saveFile(
                file,
                this.uploadDir + "/" 
                    + this.coursesDir + "/" 
                    + assignment.getCourse().getCourseId() + "/assignments/" 
                    + assignment.getAssignmentId() + "/submissions/"
                    + assignmentSubmission.getAssignmentSubmissionId() + "/" 
                    + fileName
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
        logger.debug("Successfully saved {} files for assignment submission with ID: {}", savedFiles.size(), assignmentSubmission.getAssignmentSubmissionId());
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
        logger.debug("Attempting to delete file with ID: {}", file.getFileId());

        // Delete the file from the filesystem
        if (!deleteFileHelper(file.getFilePath())) {
            logger.error("File not found on filesystem for file with ID: {}. File path: {}", file.getFileId(), file.getFilePath());
            throw new FileNotFoundException("File not found on the filesystem: " + file.getFilePath());
        }

        // Delete the file from the repo first
        filesRepository.delete(file);
        logger.debug("Successfully deleted file with ID: {}", file.getFileId());
    }


    /**
     * Gets the file extension from a file name
     * @param fileName - The file name
     * @return The file extension
     */
    private String getFileExtension(String fileName) {
        logger.trace("Getting file extension for file name: {}", fileName);
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
        logger.trace("Saving file to path: {}. Original file name: {}, Content type: {}, Size: {} bytes", path, file.getOriginalFilename(), file.getContentType(), file.getSize());

        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(path).getParent();
        if (!Files.exists(uploadPath)) {
            logger.trace("Upload directory does not exist. Attempting to create directories for path: {}", uploadPath.toString());
            Files.createDirectories(uploadPath);
        }

        // Save the file
        logger.trace("Attempting to save file to path: {}", path);
        Path filePath = Paths.get(path);
        Files.write(filePath, file.getBytes());

        logger.trace("Successfully saved file to path: {}", path);
        return filePath.toString();
    }


    /**
     * Deletes a file at the specified path
     * @param path - The path of the file to delete
     * @return True if the file was deleted, false if it did not exist
     * @throws IOException If there was an error deleting the file
     */
    private boolean deleteFileHelper(String path) throws IOException{
        logger.trace("Attempting to delete file at path: {}", path);

        Path filePath = Paths.get(path);
        return Files.deleteIfExists(filePath);
    }
}
