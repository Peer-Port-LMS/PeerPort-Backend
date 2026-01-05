package peerport.backend.dto.assignments;

import java.util.Date;
import java.util.List;

import peerport.backend.dto.FileDTO;

public class AssignmentDTO {
    public String assignmentId;
    public String courseId;
    public String name;
    public String description;
    public Date dueDate;
    public Boolean visible;
    public Date dateCreated;
    public Date dateUpdated;
    public List<FileDTO> files;
}
