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

@Service
public class AnnouncementService {

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


    AnnouncementService(FileService fileService) {
        this.fileService = fileService;
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
        // Get the course
        CourseModel course = courseService.getCourseById(courseId);

        // Check if the user is allowed to edit the course
        courseService.userAllowedToEditCourse(course);

        // Set the course to the announcement
        announcement.setCourse(course);

        // Save the announcement
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
        // Check files
        for (MultipartFile file : files) {
            if (file != null && file.getSize() > fileUploadSizeLimit) { // 5MB limit
                throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
            } 
        }
        
        // Get the course
        CourseModel course = courseService.getCourseById(courseId);

        // Check if the user is allowed to edit the course
        courseService.userAllowedToEditCourse(course);

        // Set the course to the announcement
        announcement.setCourse(course);

        // Save the files to the announcement
        if (files != null) {
            fileService.saveAnnouncementFiles(files, announcement.getAnnouncementId(), courseId);
        }

        // Save the announcement
        return announcementsRepository.save(announcement);
    }

    /**
     * Get all announcements
     * 
     * @return List of AnnouncementModels
     */
    public List<AnnouncementModel> getAllAnnouncements() {
        // Get the current users role
        UserModel user = authService.getCurrentUser();
        Role role = user.getRole();

        // If admin, return all announcements
        if (role == Role.ADMIN) {
            return announcementsRepository.findAll();
        }

        // Get all the courses for the user 
        // (Filtering is done in CourseService based on role)
        List<CourseModel> courses = courseService.getAllCourses();

        // Get all the announcements for those courses
        List<AnnouncementModel> allAnnouncements = new ArrayList<>();
        for (CourseModel course : courses) {
            allAnnouncements.addAll(course.getAnnouncements());
        }

        // Return the announcements
        return allAnnouncements;
    }

    /**
     * Get announcement by ID
     * 
     * @param announcementId - ID of the announcement to get
     * @return The AnnouncementModel
     * @throws AnnouncementNotFoundException if announcement not found (Handled in GlobalExceptionHandler)
     */
    public AnnouncementModel getAnnouncementById(String announcementId) {
        // Get the announcement by ID
        Optional<AnnouncementModel> announcement = announcementsRepository.findById(announcementId);

        // Check if its empty
        if (announcement.isEmpty()) {
            throw new AnnouncementNotFoundException(announcementId);
        }

        // Return the announcement
        return announcement.get();
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
    public AnnouncementModel updateAnnouncement(String announcementId, AnnouncementModel updatedAnnouncement, List<MultipartFile> files) throws IOException {
        // Check files
        for (MultipartFile file : files) {
            if (file != null && file.getSize() > fileUploadSizeLimit) { // 5MB limit
                throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
            } 
        }
        
        // Get the announcement by ID
        AnnouncementModel announcement = getAnnouncementById(announcementId);
        
        // Check if the user is allowed to edit the announcement
        userAllowedToEditAnnouncement(announcement);

        // Update the announcement
        announcement.setTitle(updatedAnnouncement.getTitle());
        announcement.setContent(updatedAnnouncement.getContent());

        if (files != null) {
            // Save the files to the announcement
            fileService.saveAnnouncementFiles(files, announcement.getAnnouncementId(), announcement.getCourse().getCourseId());
        }

        // Update the announcement in the database
        announcementsRepository.save(announcement);

        // Return the updated announcement
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
        // Check if the user is allowed to edit the announcement
        userAllowedToEditAnnouncement(announcement);

        // Check if patchedAnnouncement is null
        if (patchedAnnouncement == null) {
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
        // Get the announcement by ID
        AnnouncementModel announcement = getAnnouncementById(announcementId);

        // Return the updated announcement
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
    public AnnouncementModel patchAnnouncement(String announcementId, AnnouncementModel patchedAnnouncement, List<MultipartFile> files) throws IOException {
        // Check files
        for (MultipartFile file : files) {
            if (file != null && file.getSize() > fileUploadSizeLimit) { // 5MB limit
                throw new FileSizeLimitExceededException("File size exceeds limit of " + fileUploadSizeLimit + " bytes.");
            } 
        }
        
        // Get the announcement by ID
        AnnouncementModel announcement = getAnnouncementById(announcementId);

        if (files != null) {
            // Save the files to the announcement
            List<FileModel> savedFiles = fileService.saveAnnouncementFiles(files, announcement.getAnnouncementId(), announcement.getCourse().getCourseId());
            announcement.setFiles(savedFiles);

            // Update the announcement in the database
            announcementsRepository.save(announcement);
        }

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

        // Delete the announcement
        announcementsRepository.deleteById(announcementId);
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
        // Get the current user
        UserModel currentUser = authService.getCurrentUser();

        // Check if the user is admin
        if (currentUser.getRole() == Role.ADMIN) return;

        // Check if the user is an instructor for the course
        if (announcement.getCourse().getInstructors().contains(currentUser)) return;

        // If not, throw exception
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
        // Get the announcement
        AnnouncementModel announcement = getAnnouncementById(announcementId);

        // Check if user is allowed to edit
        userAllowedToEditAnnouncement(announcement);
    }
}
