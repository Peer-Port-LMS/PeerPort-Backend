package peerport.backend.exceptions.assignments;

public class AssignmentNotFoundException extends RuntimeException {
    public AssignmentNotFoundException(String assignmentId) {
        super("Assignment with ID " + assignmentId + " not found");
    }   
}
