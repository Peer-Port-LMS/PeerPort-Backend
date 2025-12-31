package peerport.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import peerport.backend.database.CoursesRepository;
import peerport.backend.dto.CourseWithInstructorsDTO;
import peerport.backend.model.CourseModel;
import peerport.backend.model.EnrollmentModel;
import peerport.backend.model.UserModel;
import peerport.backend.model.RoleModel.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private AuthService authService;

    @Autowired
    private CoursesRepository courseRepository;

    // Create Course
    public CourseModel createCourse(CourseModel course) {
        return courseRepository.save(course);
    }

    // Get All Courses
    public List<CourseWithInstructorsDTO> getAllCourses() throws IllegalArgumentException {
        // Get the user role
        Optional<UserModel> userOpt = authService.getCurrentUser();
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not authenticated");
        }

        // Get user
        UserModel user = userOpt.get();

        // Check if user is admin
        if (user.getRole() == Role.ADMIN) {
            List<CourseModel> courses = courseRepository.findAll();
            
            // Convert to DTOs
            List<CourseWithInstructorsDTO> courseDTOs = courses.stream()
                    .map(CourseModel::toCourseWithInstructorsDTO)
                    .toList();

            return courseDTOs;  
        }

        // Get the courses the user is enrolled in
        List<EnrollmentModel> enrollments = user.getEnrollments();

        // Go through the enrollments and get the courses
        List<CourseWithInstructorsDTO> courses = new ArrayList<>();
        for (EnrollmentModel enrollment : enrollments) {
            courses.add(enrollment.getCourse().toCourseWithInstructorsDTO());
        }

        return courses;
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
