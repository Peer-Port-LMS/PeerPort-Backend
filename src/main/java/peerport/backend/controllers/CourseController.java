package peerport.backend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import peerport.backend.model.CourseModel;
import peerport.backend.service.CourseService;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;
    
    // Get all courses
    @GetMapping
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
    public ResponseEntity<CourseModel> createCourse(@RequestBody CourseModel course) {
        CourseModel savedCourse = courseService.saveCourse(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse);
    }

    // Update course
    @PutMapping("/{uuid}")
    public ResponseEntity<CourseModel> updateCourse(@PathVariable String uuid, @RequestBody CourseModel course) {
        return courseService.updateCourse(uuid, course)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete course
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String uuid) {
        boolean deleted = courseService.deleteCourse(uuid);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
