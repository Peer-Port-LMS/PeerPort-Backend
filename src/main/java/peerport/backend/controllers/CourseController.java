package peerport.backend.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import peerport.backend.exceptions.FailedToParseFormDataException;
import peerport.backend.dto.courses.CourseDTO;
import peerport.backend.dto.courses.CourseWithAllDetailsDTO;
import peerport.backend.dto.courses.CourseWithInstructorsDTO;
import peerport.backend.model.CourseModel;
import peerport.backend.model.groups.OnCreate;
import peerport.backend.service.CourseService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Controller for handling course-related endpoints
 */
@RestController
@RequestMapping("/courses")
public class CourseController {

    // Services
    @Autowired
    private CourseService courseService;
    
    // Helper
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;
    
    // Environment variables 
    @Value("${file.upload-size-limit}")
    private long fileUploadSizeLimit;


    /**
     * Get all courses.
     * 
     * @return List of CourseWithInstructorsDTO
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     */
    @GetMapping
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN)")
    public ResponseEntity<List<CourseWithInstructorsDTO>> getAllCourses() {
        // Get all courses
        List<CourseModel> courses = courseService.getAllCourses();

        // Convert to DTOs
        List<CourseWithInstructorsDTO> courseDTOs = new ArrayList<>();
        for (CourseModel course : courses) {
            courseDTOs.add(course.toCourseWithInstructorsDTO());
        }

        // Return the DTOs
        return ResponseEntity.ok(courseDTOs);
    }

    /**
     * Get a course by its ID.
     * 
     * @param courseId - The ID of the course to get 
     * @return The CourseWithAllDetailsDTO of the course
     * @throws CourseNotFoundException If the course with the given ID does not exist
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseWithAllDetailsDTO> getCourseById(@PathVariable String courseId) {
        // Get and return the course
        return ResponseEntity.ok(courseService.getCourseById(courseId).toCourseWithAllDetailsDTO());
    }

    /**
     * Create a new course.
     * Only users with ADMIN or INSTRUCTOR roles can create courses.
     * 
     * @param courseJsonFromForm - The JSON string representing the course to create
     * @param image - The image file for the course
     * @return The created CourseDTO
     * @throws IOException If an error occurs while processing the image file
     * @throws FailedToParseFormDataException If the JSON parsing fails
     * @throws InvalidFileTypeException If the uploaded file type is invalid
     * @throws FileSizeLimitExceededException If the uploaded file exceeds the size limit
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to create a course
     */
    @PostMapping
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<CourseDTO> createCourse(
        @RequestPart(value="course", required=false) String courseJsonFromForm,
        @RequestPart(value="image", required=true) MultipartFile image
    ) throws IOException {
        // Convert json to CourseModel object
        CourseModel courseFromForm;

        try {
            courseFromForm = objectMapper.readValue(courseJsonFromForm, CourseModel.class);
        
        // Catch JSON parsing exceptions
        } catch (JacksonException e) {
            throw new FailedToParseFormDataException("Invalid JSON format for course data: " + courseJsonFromForm);
        }
        
        if (courseFromForm != null) {
            // Validate the courseModel
            Set<ConstraintViolation<CourseModel>> violations = validator.validate(courseFromForm, OnCreate.class, Default.class);

            // Check if the validation failed
            if (!violations.isEmpty()) {
                // Collect violation messages
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<CourseModel> violation : violations) {
                    sb.append(violation.getPropertyPath().toString())
                        .append(" ")
                        .append(violation.getMessage())
                        .append("; ");
                }
                
                // Throw exception with all violation messages
                throw new FailedToParseFormDataException("Course data validation failed: " + sb.toString());
            }
        }

        // Create the course
        CourseModel savedCourse = courseService.createCourse(courseFromForm, image);

        // Return the created course
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse.toDTO());
    }

    /**
     * Update a course with the given fields.
     * Only users with ADMIN or INSTRUCTOR roles can update courses.
     * 
     * @param courseId - The ID of the course to update
     * @param courseJsonFromForm - The JSON string representing the course fields to update
     * @param image - The image file to update for the course
     * @return The updated CourseDTO
     * @throws IOException If an error occurs while processing the image file
     * @throws FailedToParseFormDataException If the JSON parsing fails
     * @throws InvalidFileTypeException If the uploaded file type is invalid    
     * @throws CourseNotFoundException If the course with the given ID does not exist
     * @throws FileSizeLimitExceededException If the uploaded file exceeds the size limit
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to update this course
     */
    @PutMapping("/{courseId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<CourseDTO> updateCourse(
        @PathVariable String courseId,

        @RequestPart(value="course", required=false) String courseJsonFromForm,
        @RequestPart(value="image", required=false) MultipartFile image
    ) throws IOException {
        // Set default for courseFromForm
        CourseModel courseFromForm = null;

        // Try to parse the course JSON from form data
        try {
            // Convert json to CourseModel object
            courseFromForm = courseJsonFromForm != null ?
                    objectMapper.readValue(courseJsonFromForm, CourseModel.class) : null;

        // Catch JSON parsing exceptions
        } catch (JacksonException e) {
            throw new FailedToParseFormDataException("Invalid JSON format for course data: " + courseJsonFromForm);
        }

        CourseModel updatedCourse = courseService.updateCourse(courseId, courseFromForm, image);
        return ResponseEntity.ok(updatedCourse.toDTO());
    }

    /**
     * Patch a course with the given fields.
     * Only users with ADMIN or INSTRUCTOR roles can patch courses.
     * 
     * @param courseId - The ID of the course to patch
     * @param courseJsonFromForm - The JSON string representing the course fields to patch
     * @param image - The image file to update for the course
     * @return The patched CourseDTO
     * @throws IOException If an error occurs while processing the image file
     * @throws FailedToParseFormDataException If the JSON parsing fails
     * @throws InvalidFileTypeException If the uploaded file type is invalid
     * @throws CourseNotFoundException If the course with the given ID does not exist
     * @throws FileSizeLimitExceededException If the uploaded file exceeds the size limit
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to patch this course
     */
    @PatchMapping("/{courseId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<CourseDTO> patchCourse(
        @PathVariable String courseId,

        @RequestPart(value="course", required=false) String courseJsonFromForm,
        @RequestPart(value="image", required=false) MultipartFile image
    ) throws IOException {
        // Set default for courseFromForm
        CourseModel courseFromForm = null;

        // Try to parse the course JSON from form data
        try {
            // Convert json to CourseModel object
            courseFromForm = courseJsonFromForm != null ?
                    objectMapper.readValue(courseJsonFromForm, CourseModel.class) : null;

        // Catch JSON parsing exceptions
        } catch (JacksonException e) {
            throw new FailedToParseFormDataException("Invalid JSON format for course data: " + courseJsonFromForm);
        }

        // Patch the course
        CourseModel updatedCourse = courseService.patchCourse(courseId, courseFromForm, image);

        // Return the updated course
        return ResponseEntity.ok(updatedCourse.toDTO());
    }

    /**
     * Deletes a course by its ID.
     * Only users with ADMIN or INSTRUCTOR roles can delete courses.
     * 
     * @param courseId - The ID of the course to delete 
     * @return A ResponseEntity with no content
     * @throws UserNotAuthenticatedException If the user is not authenticated to perform this action
     * @throws UserNotAuthorizedException If the user is not authorized to delete this course
     */
    @DeleteMapping("/{courseId}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<Void> deleteCourse(@PathVariable String courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }
}
