package peerport.backend.dto.courses;

import java.util.ArrayList;
import java.util.List;

import peerport.backend.dto.UserDTO;
import peerport.backend.dto.announcements.AnnouncementDTO;
import peerport.backend.dto.assignments.AssignmentDTO;

public class CourseWithAllDetailsDTO extends CourseDTO {
    public UserDTO[] students;
    public UserDTO[] instructors;
    public AnnouncementDTO[] announcements;
    public AssignmentDTO[] assignments;

    public void removePrivilegedDetails() {
        List<AssignmentDTO> visibleAssignments = new ArrayList<>();
        for (AssignmentDTO assignment : assignments) {
            if (assignment.visible == true) {
                visibleAssignments.add(assignment);
            }
        }
    }
}
