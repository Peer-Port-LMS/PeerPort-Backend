package peerport.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import peerport.backend.database.AnnouncementsRepository;
import peerport.backend.exceptions.announcements.AnnouncementNotFoundException;
import peerport.backend.exceptions.courses.CourseNotFoundException;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.AnnouncementModel;
import peerport.backend.model.CourseModel;
import peerport.backend.model.FileModel;
import peerport.backend.model.RoleModel.Role;
import peerport.backend.model.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AnnouncementService {
    protected static final Logger logger = LoggerFactory.getLogger(AnnouncementService.class);


    @Autowired
    private FileService fileService;
    
    @Autowired
    private AnnouncementsRepository announcementsRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AuthService authService;


    // Environment Variables
    @Value("${file.upload-size-limit}")
    private long fileUploadSizeLimit;


    /**
     * Get all announcements
     * 
     * @return List of AnnouncementModels
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    public List<AnnouncementModel> getAllAnnouncements() {
        logger.debug("Getting all announcements for current user");

        // Get the current users role
        UserModel user = authService.getCurrentUser();
        if (user == null) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }
        Role role = user.getRole();

        // Start from all announcements in repository
        List<AnnouncementModel> announcements = announcementsRepository.findAll();

        // If admin, return all announcements
        if (role == Role.ADMIN) {
            logger.trace("User is admin, returning all announcements");
            return announcements;
        }

        // For all non-admin roles, allow announcements from user courses
        List<AnnouncementModel> visibleAnnouncements = new ArrayList<>();
        for (AnnouncementModel announcement : announcements) {
            CourseModel course = announcement.getCourse();
            boolean isEnrolled = user.getEnrollments() != null && user.getEnrollments().stream()
                .anyMatch(enrollment -> enrollment.getCourse() != null && enrollment.getCourse().equals(course));
            boolean isInstructor = course != null && course.getInstructors().contains(user);
            if (course != null && (course.getUsers().contains(user) || isEnrolled || isInstructor)) {
                visibleAnnouncements.add(announcement);
            }
        }

        // Return the announcements
        logger.debug("Returning {} announcements for user with role: {}", visibleAnnouncements.size(), role);
        return visibleAnnouncements;
    }

    /**
     * Get announcement by ID
     * 
     * @param announcementId - ID of the announcement to get
     * @return The AnnouncementModel
     * @throws AnnouncementNotFoundException if announcement not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to access (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    public AnnouncementModel getAnnouncementById(String announcementId) {
        logger.debug("Getting announcement with ID: {}", announcementId);

        UserModel user = authService.getCurrentUser();
        if (user == null) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }

        // Get the announcement by ID
        Optional<AnnouncementModel> announcement = announcementsRepository.findById(announcementId);

        // Check if its empty
        if (announcement.isEmpty()) {
            logger.warn("Announcement with ID: {} not found", announcementId);
            throw new AnnouncementNotFoundException(announcementId);
        }

        AnnouncementModel announcementModel = announcement.get();

        userAllowedToAccessAnnouncement(announcementModel);

        // Return the announcement
        logger.debug("Successfully retrieved announcement with ID: {}", announcementId);
        return announcementModel;
    }

    /**
     * Create announcement
     * 
     * @param courseId - ID of the course to create the announcement for
     * @param announcement - AnnouncementModel to create
     * @return The created AnnouncementModel
     * @throws CourseNotFoundException if course not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AnnouncementModel createAnnouncement(String courseId, AnnouncementModel announcement) {
        logger.debug("Creating announcement for course ID: {}", courseId);

        UserModel user = authService.getCurrentUser();
        if (user == null) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }

        // Get the course
        final CourseModel course;
        try {
            course = courseService.getCourseById(courseId);
        } catch (Exception exception) {
            throw new CourseNotFoundException(courseId);
        }

        if (course == null) {
            throw new UserNotAuthorizedException("User is not authorized to create announcements.");
        }

        // Check if the user is allowed to edit the course
        courseService.userAllowedToEditCourse(course);

        // Set the course to the announcement
        announcement.setCourse(course);

        // Save the announcement
        logger.debug("Saving announcement with ID: {}", announcement.getAnnouncementId());
        return announcementsRepository.save(announcement);
    }

    /**
     * Create announcement
     * 
     * @param courseId - ID of the course to create the announcement for
     * @param announcement - AnnouncementModel to create
     * @param files - List of files to attach to the announcement
     * @return The created AnnouncementModel
     * @throws IOException If there was an error saving the files
     * @throws FileSizeLimitExceededException if any file exceeds size limit
     * @throws CourseNotFoundException if course not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AnnouncementModel createAnnouncement(String courseId, AnnouncementModel announcement, List<MultipartFile> files) throws IOException {
        logger.debug("Creating announcement for course ID: {} with multipart/form-data", courseId);

        // Check files
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && file.getSize() > fileUploadSizeLimit) {
                    throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
                }
            }
        }

        // Save the announcement first to ensure ID is available for file naming
        AnnouncementModel savedAnnouncement = createAnnouncement(courseId, announcement);

        // Save the files to the announcement
        if (files != null) {
            logger.trace("Saving {} files for announcement with ID: {}", files.size(), savedAnnouncement.getAnnouncementId());
            List<FileModel> savedFiles = fileService.saveAnnouncementFiles(files, savedAnnouncement, courseId);
            announcement.getFiles().addAll(savedFiles);
            announcementsRepository.save(announcement);
        }

        // Return the announcement with any attached files
        logger.debug("Successfully created announcement with ID: {}", savedAnnouncement.getAnnouncementId());
        return announcement;
    }

    /**
     * Update announcement
     * 
     * @param announcementId - ID of the announcement to update
     * @param updatedAnnouncement - AnnouncementModel with updated fields
     * @return The updated AnnouncementModel
     * @throws AnnouncementNotFoundException if announcement not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler
     */
    @Transactional
    public AnnouncementModel updateAnnouncement(String announcementId, AnnouncementModel updatedAnnouncement) {
        logger.debug("Updating announcement with ID: {}", announcementId);

        // Get the announcement by ID
        AnnouncementModel announcement = getAnnouncementById(announcementId);
        
        // Check if the user is allowed to edit the announcement
        userAllowedToEditAnnouncement(announcement);

        // Update the announcement
        announcement.setTitle(updatedAnnouncement.getTitle());
        announcement.setContent(updatedAnnouncement.getContent());

        // Update the announcement in the database
        announcementsRepository.save(announcement);

        // Return the updated announcement
        logger.debug("Successfully updated announcement with ID: {}", announcement.getAnnouncementId());
        return announcement;
    }

    /**
     * Update announcement
     * 
     * @param announcementId - ID of the announcement to update
     * @param updatedAnnouncement - AnnouncementModel with updated fields
     * @param files - List of files to attach to the announcement
     * @return The updated AnnouncementModel
     * @throws IOException If there was an error saving the files
     * @throws FileSizeLimitExceededException if any file exceeds size limit
     * @throws AnnouncementNotFoundException if announcement not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler
     */
    @Transactional
    public AnnouncementModel updateAnnouncement(
        String announcementId,
        AnnouncementModel updatedAnnouncement,
        List<MultipartFile> files,
        List<String> removeFileIds,
        Boolean replaceAll
    ) throws IOException {
        logger.debug("Updating announcement with ID: {} with multipart/form-data", announcementId);

        // Check files
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && file.getSize() > fileUploadSizeLimit) {
                    throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
                }
            }
        }

        // Get the announcement by ID
        AnnouncementModel announcement = getAnnouncementById(announcementId);
        
        // Check if the user is allowed to edit the announcement
        userAllowedToEditAnnouncement(announcement);

        // Update the announcement
        announcement.setTitle(updatedAnnouncement.getTitle());
        announcement.setContent(updatedAnnouncement.getContent());

        // Remove / replace / add files
        applyFileChanges(announcement, files, removeFileIds, replaceAll);

        // Update the announcement in the database
        announcementsRepository.save(announcement);

        // Return the updated announcement
        logger.debug("Successfully updated announcement with ID: {}", announcement.getAnnouncementId());
        return announcement;
    }
    

    /**
     * Patch announcement
     * 
     * @param announcementId - ID of the announcement to patch
     * @param patchedAnnouncement - AnnouncementModel with fields to patch
     * @return The patched AnnouncementModel
     * @throws AnnouncementNotFoundException if announcement not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AnnouncementModel patchAnnouncement(AnnouncementModel announcement, AnnouncementModel patchedAnnouncement) {
        logger.debug("Patching announcement with ID: {}", announcement.getAnnouncementId());

        // Check if the user is allowed to edit the announcement
        userAllowedToEditAnnouncement(announcement);

        // Check if patchedAnnouncement is null
        if (patchedAnnouncement == null) {
            logger.warn("Patched announcement is null returning original announcement with ID: {}.", announcement.getAnnouncementId());
            return announcement;
        }

        // Patch the announcement
        if (patchedAnnouncement.getTitle() != null) {
            announcement.setTitle(patchedAnnouncement.getTitle());
        }
        if (patchedAnnouncement.getContent() != null) {
            announcement.setContent(patchedAnnouncement.getContent());
        }

        // Update the announcement in the database
        announcementsRepository.save(announcement);

        // Return the updated announcement
        logger.debug("Successfully patched announcement with ID: {}", announcement.getAnnouncementId());
        return announcement;
    }

    /**
     * Patch announcement
     * 
     * @param announcement - The announcement to patch
     * @param patchedAnnouncement - AnnouncementModel with fields to patch
     * @return The patched AnnouncementModel
     * @throws AnnouncementNotFoundException if announcement not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AnnouncementModel patchAnnouncement(String announcementId, AnnouncementModel patchedAnnouncement) {
        logger.debug("Patching announcement with ID: {}", announcementId);

        // Get the announcement by ID
        AnnouncementModel announcement = getAnnouncementById(announcementId);

        // Return the updated announcement
        logger.debug("Successfully patched announcement with ID: {}", announcement.getAnnouncementId());
        return patchAnnouncement(announcement, patchedAnnouncement);
    }

    /**
     * Patch announcement
     * 
     * @param announcementId - ID of the announcement to patch
     * @param patchedAnnouncement - AnnouncementModel with fields to patch
     * @param files - List of files to attach to the announcement
     * @return The patched AnnouncementModel
     * @throws IOException If there was an error saving the files
     * @throws FileSizeLimitExceededException if any file exceeds size limit (Handled in GlobalExceptionHandler)
     * @throws AnnouncementNotFoundException if announcement not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public AnnouncementModel patchAnnouncement(
        String announcementId,
        AnnouncementModel patchedAnnouncement,
        List<MultipartFile> files,
        List<String> removeFileIds,
        Boolean replaceAll
    ) throws IOException {
        logger.debug("Patching announcement with ID: {} with multipart/form-data", announcementId);

        // Check files
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && file.getSize() > fileUploadSizeLimit) {
                    throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
                }
            }
        }

        // Get the announcement by ID
        AnnouncementModel announcement = getAnnouncementById(announcementId);

        // Remove / replace / add files
        applyFileChanges(announcement, files, removeFileIds, replaceAll);

        // Update the announcement in the database
        announcementsRepository.save(announcement);

        // Return the updated announcement
        return patchAnnouncement(announcement, patchedAnnouncement);
    }


    /**
     * Delete announcement by ID
     * 
     * @param announcementId
     * @throws AnnouncementNotFoundException if announcement not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to delete (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public void deleteAnnouncement(String announcementId) {
        // Check if the user is allowed to delete the announcement
        // This will also check if the announcement exists
        userAllowedToEditAnnouncement(announcementId);

        AnnouncementModel announcement = getAnnouncementById(announcementId);

        // Delete the announcement
        announcementsRepository.deleteById(announcement.getAnnouncementId());
    }


    // Helpers //
    /**
     * Check if user is allowed to edit announcement
     * 
     * @param announcement - AnnouncementModel to check
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    public void userAllowedToEditAnnouncement(AnnouncementModel announcement) {
        logger.debug("Checking if user is allowed to edit announcement with ID: {}", announcement.getAnnouncementId());

        UserModel currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        CourseModel course = announcement.getCourse();
        if (course != null && course.getInstructors().contains(currentUser)) {
            return;
        }

        throw new UserNotAuthorizedException("User is not authorized to edit announcement with ID: " + announcement.getAnnouncementId());
    }

    /**
     * Check if user is allowed to edit announcement by ID
     * 
     * @param announcementId - ID of the announcement to check
     * @throws AnnouncementNotFoundException if announcement not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    public void userAllowedToEditAnnouncement(String announcementId) {
        logger.debug("Checking if user is allowed to edit announcement with ID: {}", announcementId);

        // Get the announcement
        AnnouncementModel announcement = getAnnouncementById(announcementId);

        // Check if user is allowed to edit
        userAllowedToEditAnnouncement(announcement);
    }


    /**
     * Check if user is allowed to access announcement
     * @param announcement - AnnouncementModel to check
     * @throws UserNotAuthorizedException if user is not authorized to access (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     */
    private void userAllowedToAccessAnnouncement(AnnouncementModel announcement) {
        logger.debug("Checking if user is allowed to access announcement with ID: {}", announcement.getAnnouncementId());
        UserModel currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        CourseModel course = announcement.getCourse();
        boolean isDirectCourseMember = course != null && course.getUsers().contains(currentUser);
        boolean isInstructor = course != null && course.getInstructors().contains(currentUser);
        boolean isEnrolled = currentUser.getEnrollments() != null && currentUser.getEnrollments().stream()
            .anyMatch(enrollment -> enrollment.getCourse() != null && enrollment.getCourse().equals(course));

        if (isDirectCourseMember || isInstructor || isEnrolled) {
            return;
        }

        throw new UserNotAuthorizedException("User is not authorized to access announcement with ID: " + announcement.getAnnouncementId());
    }


    /**
     * Apply file changes to announcement
     * 
     * @param announcement - The announcement to apply changes to
     * @param filesToAdd - The files to add
     * @param removeFileIds - The IDs of files to remove
     * @param replaceAll - Whether to replace all existing files
     * @throws IOException If there was an error saving the files
     */
    private void applyFileChanges(
        AnnouncementModel announcement,
        List<MultipartFile> filesToAdd,
        List<String> removeFileIds,
        Boolean replaceAll
    ) throws IOException {
        logger.debug("Applying file changes to announcement with ID: {}", announcement.getAnnouncementId());

        // Ensure collection exists
        if (announcement.getFiles() == null) {
            announcement.setFiles(new ArrayList<>());
        }

        // If replaceAll=true, delete everything first
        if (Boolean.TRUE.equals(replaceAll)) {
            logger.trace("Removing all files for announcement with ID: {}", announcement.getAnnouncementId());
            for (FileModel existing : new ArrayList<>(announcement.getFiles())) {
                fileService.deleteFile(existing);
            }
            announcement.getFiles().clear();
        } else if (removeFileIds != null) {
            logger.trace("Removing files with IDs: {} for announcement with ID: {}", removeFileIds, announcement.getAnnouncementId());
            // Remove specific files
            List<FileModel> remaining = new ArrayList<>();
            for (FileModel existing : announcement.getFiles()) {
                String fileId = existing.getFileId();
                boolean shouldRemove = fileId != null && removeFileIds.contains(fileId);
                if (shouldRemove) {
                    fileService.deleteFile(existing);
                } else {
                    remaining.add(existing);
                }
            }
            announcement.setFiles(remaining);
        }

        // Add new files
        if (filesToAdd != null) {
            logger.trace("Adding {} files to announcement with ID: {}", filesToAdd.size(), announcement.getAnnouncementId());
            List<FileModel> savedFiles = fileService.saveAnnouncementFiles(
                filesToAdd,
                announcement,
                announcement.getCourse().getCourseId()
            );
            announcement.getFiles().addAll(savedFiles);
        }
    }
}
