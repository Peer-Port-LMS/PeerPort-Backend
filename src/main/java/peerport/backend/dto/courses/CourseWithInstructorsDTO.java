package peerport.backend.dto.courses;

import peerport.backend.dto.UserDTO;

public class CourseWithInstructorsDTO extends CourseDTO {
    public UserDTO[] instructors;
}
