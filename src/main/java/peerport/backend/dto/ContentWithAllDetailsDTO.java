package peerport.backend.dto;

import java.util.List;

public class ContentWithAllDetailsDTO extends ContentDTO {
    public CourseDTO course;
    public List<ContentDTO> subContent;
}
