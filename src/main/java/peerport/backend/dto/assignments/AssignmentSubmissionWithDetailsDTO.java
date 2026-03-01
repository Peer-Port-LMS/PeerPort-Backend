package peerport.backend.dto.assignments;

import java.util.List;
import java.util.Optional;

import peerport.backend.dto.FileDTO;
import peerport.backend.dto.GradeDTO;
import peerport.backend.dto.UserDTO;

public class AssignmentSubmissionWithDetailsDTO extends AssignmentSubmissionDTO {
    public UserDTO user;
    public AssignmentDTO assignment;
    public Optional<GradeDTO> grade;
    public List<FileDTO> submittedFiles;
}