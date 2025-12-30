package peerport.backend.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.groups.Default;
import peerport.backend.dto.CourseDTO;
import peerport.backend.dto.CourseWithAllDetailsDTO;
import peerport.backend.model.CourseModel;
import peerport.backend.model.FileModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.groups.OnCreate;
import peerport.backend.service.AuthService;
import peerport.backend.service.CourseService;
import peerport.backend.service.FileService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/courses")
public class CourseController {

    // Services
    @Autowired
    private CourseService courseService;

    @Autowired
    private FileService fileService;

    @Autowired
    private AuthService authService;
    
    // Helper
    @Autowired
    private ObjectMapper objectMapper;

    
    // Enviroment vairables
    @Value("${file.upload-size-limit}")
    private long fileUploadSizeLimit;


    // Get all courses
    @GetMapping
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN)")
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        // Get all courses
        List<CourseModel> courses = courseService.getAllCourses();

        // Convert courses to DTOs
        List<CourseDTO> courseDTOs = courses.stream().map(CourseModel::toDTO).toList();
        return ResponseEntity.ok(courseDTOs);
    }

    // Get course by ID
    @GetMapping("/{uuid}")
    public ResponseEntity<CourseWithAllDetailsDTO> getCourseById(@PathVariable String uuid) {
        // Get the course by ID and convert to DTO
        return courseService.getCourseById(uuid)
                .map(course -> ResponseEntity.ok(course.toCourseWithAllDetailsDTO()))
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new course - Only takes formData add JSON option later
    @PostMapping
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<CourseDTO> createCourse(
        // Form data
        @Validated({OnCreate.class, Default.class}) @RequestPart(value="course", required=false) CourseModel courseFromForm,
        @RequestPart(value="image", required=false) MultipartFile image
    ) {
        // Get the course from either FormData or JSON body
        CourseModel course = courseFromForm;
        
        // Validate that course data was provided
        if (course == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        // Get the current user
        Optional<UserModel> currentUser = authService.getCurrentUser();

        // Check if the user is present
        if (!currentUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Validate the image
        if (image != null && image.getSize() > fileUploadSizeLimit) { // 5MB limit
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        try {
            // Add the user as an instructor before saving
            course.addInstructor(currentUser.get());
            
            // Create the course once
            CourseModel savedCourse = courseService.createCourse(course);
        
            // Save the image if needed
            if (image != null) {
                try {
                    FileModel savedCourseImage = fileService.saveCourseImage(image, savedCourse.getCourseId());
                    savedCourse.setImage(savedCourseImage);
                    // Update course with image reference
                    savedCourse = courseService.updateCourse(savedCourse.getCourseId(), savedCourse);
                } catch (IOException e) {
                    // If image save fails, delete the created course to avoid orphans
                    courseService.deleteCourse(savedCourse.getCourseId());
                    throw e;
                }
            }

            // Return the created course
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse.toDTO());

        // Catch IO exceptions
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Update course - Only takes formData add JSON option later
    @PutMapping("/{uuid}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<CourseDTO> updateCourse(
        @PathVariable String uuid,

        // Form data
        @RequestPart(value="course", required=false) CourseModel courseFromForm,
        @RequestPart(value="image", required=false) MultipartFile image
    ) {
        // Get the course from either FormData or JSON body
        CourseModel course = courseFromForm;
        
        // Validate that course data was provided
        if (course == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        // Validate the image
        if (image != null && image.getSize() > fileUploadSizeLimit) { // 5MB limit
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        try {
            // Try to update the course
            CourseModel updatedCourse = courseService.updateCourse(uuid, course);

            // Save the image if needed
            if (image != null) {
                FileModel savedCourseImage = fileService.saveCourseImage(image, updatedCourse.getCourseId());
                // Delete old image to prevent orphans
                FileModel oldImage = updatedCourse.getImage();
                if (oldImage != null) {
                    fileService.deleteFile(oldImage);
                }
                updatedCourse.setImage(savedCourseImage);
                updatedCourse = courseService.updateCourse(uuid, updatedCourse);
            }

            // Return the updated course
            return ResponseEntity.ok(updatedCourse.toDTO());

        // Catch illegal argument exception and return 404 Not Found
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();

        // Catch IO exceptions
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Partially update course - Only takes formData add JSON option later
    @PatchMapping("/{uuid}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<CourseDTO> patchCourse(
        @PathVariable String uuid,

        // Form data
        @RequestPart(value="course", required=false) String courseJsonFromForm,
        @RequestPart(value="image", required=false) MultipartFile image
    ) {
        // Set default for courseFromForm
        CourseModel courseFromForm = null;

        // Try to parse the course JSON from form data
        try {
            // Convert json to CourseModel object
            courseFromForm = courseJsonFromForm != null ?
                    objectMapper.readValue(courseJsonFromForm, CourseModel.class) : null;

        // Catch JSON parsing exceptions
        } catch (JacksonException e) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Get the course from the request or database
            CourseModel course;
            CourseModel updatedCourse;

            // The request contained course data
            if (courseFromForm != null) {
                course = courseFromForm;

                // Try to update the course
                updatedCourse = courseService.patchCourse(uuid, course);

            // The request did not contain course data
            } else {
                // Get existing course from database
                Optional<CourseModel> existingCourseOpt = courseService.getCourseById(uuid);
                if (existingCourseOpt.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                updatedCourse = existingCourseOpt.get();
            }

            // Validate the image
            if (image != null && image.getSize() > fileUploadSizeLimit) { // 5MB limit
                return ResponseEntity.badRequest().build();
            }

            // Save the image if needed
            if (image != null) {
                FileModel savedCourseImage = fileService.saveCourseImage(image, updatedCourse.getCourseId());
                FileModel oldImage = updatedCourse.getImage();
                if (oldImage != null) {
                    fileService.deleteFile(oldImage);
                }
                updatedCourse.setImage(savedCourseImage);
                updatedCourse = courseService.patchCourse(uuid, updatedCourse);
            }

            // Return the updated course
            return ResponseEntity.ok(updatedCourse.toDTO());

        // Catch illegal argument exception and return 404 Not Found
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();

        // Catch IO exceptions
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Delete course
    @DeleteMapping("/{uuid}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<Void> deleteCourse(@PathVariable String uuid) {
        boolean deleted = courseService.deleteCourse(uuid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
