package peerport.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import peerport.backend.database.CoursesRepository;
import peerport.backend.model.CourseModel;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CoursesRepository courseRepository;

    // Create Course
    public CourseModel createCourse(CourseModel course) {
        return courseRepository.save(course);
    }

    // Get All Courses
    public List<CourseModel> getAllCourses() {
        return courseRepository.findAll();
    }

    // Get Course by ID
    public Optional<CourseModel> getCourseById(String uuid) {
        return courseRepository.findById(uuid);
    }

    // Delete Course by ID
    public boolean deleteCourse(String uuid) {
        if (courseRepository.existsById(uuid)) {
            courseRepository.deleteById(uuid);
            return true;
        }
        return false;
    }

    // Update Course
    public CourseModel updateCourse(String uuid, CourseModel updatedCourse) throws IllegalArgumentException {
        // Get the course
        Optional<CourseModel> existingCourse = courseRepository.findById(uuid);

        // Check if course exists
        if (!existingCourse.isPresent()) {
            throw new IllegalArgumentException("Course not found");
        }

        // Get the existing course
        CourseModel course = existingCourse.get();

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

    // Patch / partial update course
    public CourseModel patchCourse(String uuid, CourseModel patchedCourse) throws IllegalArgumentException {
        // Get the course
        Optional<CourseModel> existingCourse = courseRepository.findById(uuid);

        // Check if course exists
        if (!existingCourse.isPresent()) {
            throw new IllegalArgumentException("Course not found");
        }

        // Get the existing course
        CourseModel course = existingCourse.get();

        // Patch the course fields
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
}
