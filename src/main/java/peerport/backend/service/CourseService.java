package peerport.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import peerport.backend.database.CoursesRepository;
import peerport.backend.exceptions.courses.CourseNotFoundException;
import peerport.backend.exceptions.files.FileSizeLimitExceededException;
import peerport.backend.exceptions.users.UserNotAuthenticatedException;
import peerport.backend.exceptions.users.UserNotAuthorizedException;
import peerport.backend.model.CourseModel;
import peerport.backend.model.EnrollmentModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling course-related operations
 */
@Service
public class CourseService {
    protected static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private CoursesRepository courseRepository;

    @Autowired 
    private FileService fileService;


    // Environment variables 
    @Value("${file.upload-size-limit}")
    private long fileUploadSizeLimit;


    /**
     * Gets all courses the current user is enrolled in.
     * If the user is an admin, gets all courses.
     * If the user is an instructor, gets all courses they are teaching.
     * 
     * @return List of CourseModels
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     */
    public List<CourseModel> getAllCourses() {
        logger.debug("Attempting to retrieve all courses");

        // Get the user role
        UserModel user = authService.getCurrentUser();
        Role role = user.getRole();

        // Check if user is admin
        if (role == Role.ADMIN) {
            logger.debug("User is admin, retrieving all courses");
            return courseRepository.findAll();

        // Check if user is instructor
        } else if (role == Role.INSTRUCTOR) {
            logger.trace("User is instructor, retrieving courses they teach and are enrolled in");

            // Get the courses the instructor is teaching
            List<CourseModel> courses = new ArrayList<>(user.getTaughtCourses());

            // Get the courses the user is enrolled in
            List<EnrollmentModel> enrollments = user.getEnrollments();
            for (EnrollmentModel enrollment : enrollments) {
                logger.trace("Adding course with ID: {} from enrollment with ID: {}", enrollment.getCourse().getCourseId(), enrollment.getEnrollmentId());
                courses.add(enrollment.getCourse());
            }

            // Return the courses
            logger.debug("Successfully retrieved {} courses for instructor with ID: {}", courses.size(), user.getUserId());
            return courses;

        // Default catch all in case new role added later
        // Treat as student
        } else {
            logger.debug("User is student, retrieving courses they are enrolled in");

            // Get the courses the user is enrolled in
            List<EnrollmentModel> enrollments = user.getEnrollments();

            // Go through the enrollments and get the courses
            List<CourseModel> courses = new ArrayList<>();
            for (EnrollmentModel enrollment : enrollments) {
                logger.trace("Adding course with ID: {} from enrollment with ID: {}", enrollment.getCourse().getCourseId(), enrollment.getEnrollmentId());
                courses.add(enrollment.getCourse());
            }

            // Return the courses
            logger.debug("Successfully retrieved {} courses for student with ID: {}", courses.size(), user.getUserId());
            return courses;
        }
    }

    /** 
     * Gets a course by its ID.
     * Users must be authorized to access the course based on their role:
     * - ADMIN: Can access any course
     * - INSTRUCTOR: Can access courses they teach or are enrolled in
     * - STUDENT: Can only access courses they are enrolled in
     * 
     * @param courseId - The ID of the course to get
     * @return The CourseModel if found and user is authorized
     * @throws CourseNotFoundException If the course is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to access the course (Handled in GlobalExceptionHandler)
     */
    public CourseModel getCourseById(String courseId) {
        logger.debug("Attempting to retrieve course with ID: {}", courseId);

        // Get the course from the repository
        Optional<CourseModel> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            logger.warn("Course with ID: {} not found", courseId);
            throw new CourseNotFoundException(courseId);
        }
        CourseModel course = courseOpt.get();
        
        // Check if the user is allowed to access the course
        userAllowedToAccessCourse(course);

        // Return the course
        logger.debug("Successfully retrieved course with ID: {}", courseId);
        return course;
    }

    // Create Course
    /**
     * Creates a new course.
     * 
     * @param course - The course model to create
     * @return The created CourseModel
     * @param image - The image file to upload for the course
     * @throws IOException If there was an error saving the file
     * @throws FileSizeLimitExceededException If the file size exceeds the limit (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public CourseModel createCourse(CourseModel course, MultipartFile image) throws IOException {
        logger.debug("Attempting to create course with name: {}", course.getName());

        // Validate the image
        if (image != null && image.getSize() > fileUploadSizeLimit) {
            throw new FileSizeLimitExceededException("File size exceeds the limit of " + fileUploadSizeLimit + " bytes.");
        }

        // Get the current user
        UserModel currentUser = authService.getCurrentUser();

        // Add the current user as an instructor. Only instructors and admins can create courses,
        // so we can safely assume they should be added as instructors.
        course.addInstructor(currentUser);

        // Save the course first to generate an ID
        course = courseRepository.save(course);

        // Save the course image if it exists
        if (image != null) {
            logger.trace("Saving image for course with ID: {}", course.getCourseId());
            course.setImage(fileService.saveCourseImage(image, course.getCourseId()));
            courseRepository.save(course);
        }

        logger.debug("Successfully created course with ID: {}", course.getCourseId());
        return course;
    }

    /**
     * Updates a course with the given fields.
     * 
     * @param uuid - The ID of the course to update
     * @param updatedCourse - The course model with the updated fields
     * @return The updated CourseModel
     * @throws CourseNotFoundException If the course is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit the course (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public CourseModel updateCourse(String uuid, CourseModel updatedCourse) {
        logger.debug("Attempting to update course with ID: {}", uuid);

        // Get the existing course
        CourseModel course = getCourseById(uuid);

        // Check if the user is allowed to edit the course
        userAllowedToEditCourse(course);

        // Update the course fields
        course.setName(updatedCourse.getName());
        course.setCourseCode(updatedCourse.getCourseCode());
        course.setIsOpen(updatedCourse.getIsOpen());
        course.setDescription(updatedCourse.getDescription());
        course.setStartDate(updatedCourse.getStartDate());
        course.setEndDate(updatedCourse.getEndDate());
        
        // Save the updated course
        courseRepository.save(course);
        logger.debug("Successfully updated course with ID: {}", uuid);
        return course;
    }

    /**
     * Updates a course including updating its image.
     * 
     * @param courseId The ID of the course to update
     * @param updatedCourse The course model with the updated fields
     * @param image The new image file to upload
     * @return The updated CourseModel
     * @throws IOException If there was an error saving the file
     * @throws FileSizeLimitExceededException If the file size exceeds the limit (Handled in GlobalExceptionHandler)
     * @throws CourseNotFoundException If the course is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit the course (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public CourseModel updateCourse(String courseId, CourseModel updatedCourse, MultipartFile image) throws IOException {
        logger.debug("Attempting to update course with ID: {} and update its image", courseId);

        // Verify the image is valid
        if (image != null && image.getSize() > fileUploadSizeLimit) {
            throw new FileSizeLimitExceededException("File size exceeds the limit of " + fileUploadSizeLimit + " bytes.");
        }

        // Update the image first if it exists
        if (image != null) {
            logger.trace("Updating image for course with ID: {}", courseId);

            // Get the course thats in the DB
            CourseModel course = getCourseById(courseId);

            // Save the courses image
            course.setImage(fileService.saveCourseImage(image, courseId));
            courseRepository.save(course);
            logger.debug("Successfully updated image for course with ID: {}", courseId);
        }

        // Update the course fields
        logger.debug("Updating fields for course with ID: {}", courseId);
        return updateCourse(courseId, updatedCourse);
    }


    /**
     * Patches a course with the given fields.
     * 
     * @param courseId
     * @param patchedCourse
     * @return The patched CourseModel
     * @throws CourseNotFoundException If the course is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit the course (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public CourseModel patchCourse(CourseModel course, CourseModel patchedCourse) {
        logger.debug("Attempting to patch course with ID: {}", course.getCourseId());

        // Check if the user is allowed to edit the course
        userAllowedToEditCourse(course);

        // Patch the fields that are not null
        if (patchedCourse.getName() != null) {
            course.setName(patchedCourse.getName());
        }
        if (patchedCourse.getCourseCode() != null) {
            course.setCourseCode(patchedCourse.getCourseCode());
        }
        if (patchedCourse.getIsOpen() != null) {
            course.setIsOpen(patchedCourse.getIsOpen());
        }
        if (patchedCourse.getDescription() != null) {
            course.setDescription(patchedCourse.getDescription());
        }
        if (patchedCourse.getStartDate() != null) {
            course.setStartDate(patchedCourse.getStartDate());
        }
        if (patchedCourse.getEndDate() != null) {
            course.setEndDate(patchedCourse.getEndDate());
        }

        // Save the patched course
        courseRepository.save(course);
        logger.debug("Successfully patched course with ID: {}", course.getCourseId());
        return course;
    }

    /**
     * Patches a course with the given fields.
     * 
     * @param courseId The ID of the course to patch
     * @param patchedCourse The course model with the fields to patch
     * @return The patched CourseModel
     * @throws CourseNotFoundException If the course is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit the course (Handled in GlobalExceptionHandler)
     * 
     */
    public CourseModel patchCourse(String courseId, CourseModel patchedCourse) {
        logger.debug("Attempting to patch course with ID: {}", courseId);

        // Get the existing course
        CourseModel course = getCourseById(courseId);

        // Patch the course fields
        return patchCourse(course, patchedCourse);
    }

    /**
     * Patches a course including updating its image.
     * 
     * @param courseId - The ID of the course to patch
     * @param patchedCourse - The course model with the fields to patch
     * @param image - The new image file to upload
     * @return The patched CourseModel
     * @throws IOException If there was an error saving the file
     * @throws FileSizeLimitExceededException If the file size exceeds the limit (Handled in GlobalExceptionHandler)
     * @throws CourseNotFoundException If the course is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit the course (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public CourseModel patchCourse(String courseId, CourseModel patchedCourse, MultipartFile image) throws IOException {
        logger.debug("Attempting to patch course with ID: {} and update its image", courseId);

        // Verify the image is valid
        if (image != null && image.getSize() > fileUploadSizeLimit) {
            throw new FileSizeLimitExceededException("File size exceeds the limit of " + fileUploadSizeLimit + " bytes.");
        }

        // Get the course thats in the DB
        CourseModel course = getCourseById(courseId);

        // Update the image first if it exists
        if (image != null) {
            logger.trace("Updating image for course with ID: {}", courseId);

            // Save the courses image
            course.setImage(fileService.saveCourseImage(image, courseId));
            courseRepository.save(course);
            logger.debug("Successfully updated image for course with ID: {}", courseId);
        }

        // Patch the course fields
        return patchCourse(course, patchedCourse);
    }

    /**
     * Deletes a course by its ID.
     * 
     * @param courseId - The ID of the course to delete
     * @throws CourseNotFoundException If the course is not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException If the user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException If the user is not authorized to delete the course (Handled in GlobalExceptionHandler)
     */
    @Transactional
    public void deleteCourse(String courseId) {
        logger.debug("Attempting to delete course with ID: {}", courseId);
        
        // Check if the user is allowed to delete the course
        // This will throw CourseNotFoundException if course not found
        userAllowedToEditCourse(courseId);

        // Delete the course
        courseRepository.deleteById(courseId);
        logger.debug("Successfully deleted course with ID: {}", courseId);
    }


    // Helpers //
    /**
     * Checks if the current user is allowed to edit the given course.
     * 
     * @param course The course to check permissions for
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit the course (Handled in GlobalExceptionHandler)
     */
    public void userAllowedToEditCourse(CourseModel course) {
        logger.debug("Checking if user is allowed to edit course with ID: {}", course.getCourseId());

        // Get the current user
        UserModel currentUser = authService.getCurrentUser();

        // Check if user is admin
        if (currentUser.getRole() == Role.ADMIN) {
            logger.debug("User is admin, allowed to edit course with ID: {}", course.getCourseId());
            return;
        }

        // Check if user is an instructor for the course
        if (course.getInstructors().contains(currentUser)) {
            logger.debug("User is an instructor for the course, allowed to edit course with ID: {}", course.getCourseId());
            return;
        }

        // User is not allowed to edit the course
        logger.warn("User with ID: {} is not authorized to edit course with ID: {}", currentUser.getUserId(), course.getCourseId());
        throw new UserNotAuthorizedException("User is not authorized to edit course with ID: " + course.getCourseId());
    }

    /**
     * Checks if the current user is allowed to edit the course with the given ID.
     * 
     * @param courseId The ID of the course to check permissions for
     * @throws CourseNotFoundException if course not found (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to edit the course (Handled in GlobalExceptionHandler)
     */
    public void userAllowedToEditCourse(String courseId) {
        logger.debug("Checking if user is allowed to edit course with ID: {}", courseId);

        // Get the course
        CourseModel course = getCourseById(courseId);

        // Check if user is allowed to edit the course
        userAllowedToEditCourse(course);
        logger.debug("User is allowed to edit course with ID: {}", courseId);
    }

    /**
     * Checks if the current user is allowed to access the given course.
     * ADMIN users can access any course.
     * INSTRUCTOR users can access courses they teach or are enrolled in.
     * STUDENT users can only access courses they are enrolled in.
     * 
     * @param course The course to check permissions for
     * @throws UserNotAuthenticatedException if user is not authenticated (Handled in GlobalExceptionHandler)
     * @throws UserNotAuthorizedException if user is not authorized to access the course (Handled in GlobalExceptionHandler)
     */
    public void userAllowedToAccessCourse(CourseModel course) {
        logger.debug("Checking if user is allowed to access course with ID: {}", course.getCourseId());

        // Get the current user
        UserModel currentUser = authService.getCurrentUser();
        Role role = currentUser.getRole();

        // Check if user is admin
        if (role == Role.ADMIN) {
            logger.debug("User is admin, allowed to access course with ID: {}", course.getCourseId());
            return;
        }

        // Check if user is an instructor for the course
        if (role == Role.INSTRUCTOR) {
            logger.trace("User is instructor, checking if they teach or are enrolled in the course with ID: {}", course.getCourseId());

            // Check if user is teaching this course
            if (course.getInstructors().contains(currentUser)) {
                logger.debug("User is an instructor for the course, allowed to access course with ID: {}", course.getCourseId());
                return;
            }

            // Check if user is enrolled in this course
            List<EnrollmentModel> enrollments = currentUser.getEnrollments();
            for (EnrollmentModel enrollment : enrollments) {
                if (enrollment.getCourse().equals(course)) {
                    logger.debug("User is enrolled in the course, allowed to access course with ID: {}", course.getCourseId());
                    return;
                }
            }
        }

        // For students or users not matching the above conditions
        // Check if user is enrolled in the course
        List<EnrollmentModel> enrollments = currentUser.getEnrollments();
        for (EnrollmentModel enrollment : enrollments) {
            logger.trace("Checking enrollment with ID: {} for course with ID: {}", enrollment.getEnrollmentId(), course.getCourseId());
            if (enrollment.getCourse().equals(course)) {
                logger.debug("User is enrolled in the course, allowed to access course with ID: {}", course.getCourseId());
                return;
            }
        }

        // User is not allowed to access the course
        logger.warn("User with ID: {} is not authorized to access course with ID: {}", currentUser.getUserId(), course.getCourseId());
        throw new UserNotAuthorizedException("User is not authorized to access course with ID: " + course.getCourseId());
    }
}
