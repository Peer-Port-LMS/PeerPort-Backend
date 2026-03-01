package peerport.backend.exceptions.grades;

public class GradeNotFoundException extends RuntimeException {
    public GradeNotFoundException(String gradeId) {
        super("Grade with ID: " + gradeId + " not found");
    }
}
