package peerport.backend.dto.content;

import java.util.List;

import peerport.backend.dto.courses.CourseDTO;

public class ContentWithAllDetailsDTO extends ContentDTO {
    public CourseDTO course;
    public List<ContentDTO> subContent;
}
