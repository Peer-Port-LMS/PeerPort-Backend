package peerport.backend.dto.courses;

import peerport.backend.dto.UserDTO;
import peerport.backend.dto.announcements.AnnouncementDTO;
import peerport.backend.dto.assignments.AssignmentDTO;

public class CourseWithAllDetailsDTO extends CourseDTO {
    public UserDTO[] students;
    public UserDTO[] instructors;
    public AnnouncementDTO[] announcements;
    public AssignmentDTO[] assignments;
}
