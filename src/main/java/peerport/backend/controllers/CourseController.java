package peerport.backend.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import peerport.backend.dto.CourseDTO;
import peerport.backend.model.CourseModel;
import peerport.backend.model.UserModel;
import peerport.backend.service.AuthService;
import peerport.backend.service.CourseService;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private AuthService authService;
    
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
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable String uuid) {
        // Get the course by ID and convert to DTO
        return courseService.getCourseById(uuid)
                .map(course -> ResponseEntity.ok(course.toDTO()))
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new course
    @PostMapping
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<CourseDTO> createCourse(@Valid @RequestBody CourseModel course) {
        // Get the current user
        Optional<UserModel> currentUser = authService.getCurrentUser();

        // Check if the user is present
        if (!currentUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Create the course
        CourseModel savedCourse = courseService.createCourse(course);

        // Addd the user as an instructor
        savedCourse.addInstructor(currentUser.get());

        // Save the course again
        savedCourse = courseService.createCourse(savedCourse);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse.toDTO());
    }

    // Update course
    @PutMapping("/{uuid}")
    @PreAuthorize("@authService.hasAnyRole(@authService.ADMIN, @authService.INSTRUCTOR)")
    public ResponseEntity<CourseDTO> updateCourse(@PathVariable String uuid, @Valid @RequestBody CourseModel course) {
        // Update the course and convert to DTO
        return courseService.updateCourse(uuid, course)
                .map(updatedCourse -> ResponseEntity.ok(updatedCourse.toDTO()))
                .orElse(ResponseEntity.notFound().build());
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
