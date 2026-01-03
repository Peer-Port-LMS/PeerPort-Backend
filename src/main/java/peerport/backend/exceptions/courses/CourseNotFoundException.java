package peerport.backend.exceptions.courses;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(String courseId) {
        super("Course with ID " + courseId + " not found");
    }
}
