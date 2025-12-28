package peerport.backend.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<List<CourseModel>> getAllCourses() {
        List<CourseModel> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    // Get course by ID
    @GetMapping("/{uuid}")
    public ResponseEntity<CourseModel> getCourseById(@PathVariable String uuid) {
        return courseService.getCourseById(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new course
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<CourseModel> createCourse(@Valid @RequestBody CourseModel course) {
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

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse);
    }

    // Update course
    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<CourseModel> updateCourse(@PathVariable String uuid, @Valid @RequestBody CourseModel course) {
        return courseService.updateCourse(uuid, course)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete course
    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> deleteCourse(@PathVariable String uuid) {
        boolean deleted = courseService.deleteCourse(uuid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
