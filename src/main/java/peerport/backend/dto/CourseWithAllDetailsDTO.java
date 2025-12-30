package peerport.backend.dto;

public class CourseWithAllDetailsDTO extends CourseDTO {
    public UserDTO[] students;
    public UserDTO[] instructors;
    public AnnouncementDTO[] announcements;
    public AssignmentDTO[] assignments;
}
