package peerport.backend.exceptions.assignmentSubmissions;

public class AssignmentSubmissionNotFoundException extends RuntimeException {
    public AssignmentSubmissionNotFoundException(String assignmentSubmissionId) {
        super("Assignment submission not found with ID: " + assignmentSubmissionId);
    }
}
