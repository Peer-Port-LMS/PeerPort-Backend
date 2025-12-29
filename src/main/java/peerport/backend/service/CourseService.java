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

    // Create or Update Course
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
    public Optional<CourseModel> updateCourse(String uuid, CourseModel updatedCourse) {
        return courseRepository.findById(uuid).map(course -> {
            course.setName(updatedCourse.getName());
            course.setCourseCode(updatedCourse.getCourseCode());
            course.setIsOpen(updatedCourse.getIsOpen());
            course.setDescription(updatedCourse.getDescription());
            course.setStartDate(updatedCourse.getStartDate());
            course.setEndDate(updatedCourse.getEndDate());
            
            return courseRepository.save(course);
        });
    }
}
