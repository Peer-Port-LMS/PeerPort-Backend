package peerport.backend.service;

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
        // Get the user role
        UserModel user = authService.getCurrentUser();
        Role role = user.getRole();

        // Check if user is admin
        if (role == Role.ADMIN) {
            return courseRepository.findAll();

        // Check if user is instructor
        } else if (role == Role.INSTRUCTOR) {
            // Get the courses the instructor is teaching
            List<CourseModel> courses = new ArrayList<>(user.getTaughtCourses());

            // Get the courses the user is enrolled in
            List<EnrollmentModel> enrollments = user.getEnrollments();
            for (EnrollmentModel enrollment : enrollments) {
                courses.add(enrollment.getCourse());
            }

            // Return the courses
            return courses;

        // Default catch all in case new role added later
        // Treat as student
        } else {
            // Get the courses the user is enrolled in
            List<EnrollmentModel> enrollments = user.getEnrollments();

            // Go through the enrollments and get the courses
            List<CourseModel> courses = new ArrayList<>();
            for (EnrollmentModel enrollment : enrollments) {
                courses.add(enrollment.getCourse());
            }

            // Return the courses
            return courses;
        }
    }

    /** 
     * Gets a course by its ID.
     * 
     * @param courseId - The ID of the course to get
     * @return An Optional containing the CourseModel if found, or empty if not found
     * @throws CourseNotFoundException If the course is not found (Handled in GlobalExceptionHandler)
     */
    public CourseModel getCourseById(String courseId) {
        Optional<CourseModel> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            throw new CourseNotFoundException(courseId);
        }
        return courseOpt.get();
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
        // Validate the image
        if (image != null && image.getSize() > fileUploadSizeLimit) { // 5MB limit
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
            course.setImage(fileService.saveCourseImage(image, course.getCourseId()));
            courseRepository.save(course);
        }

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
        // Verify the image is valid
        if (image != null && image.getSize() > fileUploadSizeLimit) { // 5MB limit
            throw new FileSizeLimitExceededException("File size exceeds the limit of " + fileUploadSizeLimit + " bytes.");
        }

        // Update the image first if it exists
        if (image != null) {
            // Get the course thats in the DB
            CourseModel course = getCourseById(courseId);

            // Save the courses image
            course.setImage(fileService.saveCourseImage(image, courseId));
            courseRepository.save(course);
        }

        // Update the course fields
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
        // Verify the image is valid
        if (image != null && image.getSize() > fileUploadSizeLimit) { // 5MB limit
            throw new FileSizeLimitExceededException("File size exceeds the limit of " + fileUploadSizeLimit + " bytes.");
        }

        // Get the course thats in the DB
        CourseModel course = getCourseById(courseId);

        // Update the image first if it exists
        if (image != null) {
            // Save the courses image
            course.setImage(fileService.saveCourseImage(image, courseId));
            courseRepository.save(course);
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
        // Check if the user is allowed to delete the course
        // This will throw CourseNotFoundException if course not found
        userAllowedToEditCourse(courseId);

        // Delete the course
        courseRepository.deleteById(courseId);
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
        // Get the current user
        UserModel currentUser = authService.getCurrentUser();

        // Check if user is admin
        if (currentUser.getRole() == Role.ADMIN) return;

        // Check if user is an instructor for the course
        if (course.getInstructors().contains(currentUser)) return;

        // User is not allowed to edit the course
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
        // Get the course
        CourseModel course = getCourseById(courseId);

        // Check if user is allowed to edit the course
        userAllowedToEditCourse(course);
    }

}
