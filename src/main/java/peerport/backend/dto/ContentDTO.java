package peerport.backend.dto;

import java.util.Date;
import java.util.List;

public class ContentDTO {
    public String contentId;
    public String title;
    public String description;
    public Boolean visible;
    public Date dateCreated;
    public Date dateUpdated;
    public List<FileDTO> files;
}
