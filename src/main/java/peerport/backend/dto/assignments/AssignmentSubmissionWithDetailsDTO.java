package peerport.backend.dto.assignments;

import java.util.List;

import peerport.backend.dto.FileDTO;
import peerport.backend.dto.UserDTO;

public class AssignmentSubmissionWithDetailsDTO extends AssignmentSubmissionDTO {
    public UserDTO user;
    public AssignmentDTO assignment;
    public List<FileDTO> submittedFiles;
}