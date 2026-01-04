package peerport.backend.dto.content;

import java.util.Date;
import java.util.List;

import peerport.backend.dto.FileDTO;

public class ContentDTO {
    public String contentId;
    public String title;
    public String description;
    public Boolean visible;
    public Date dateCreated;
    public Date dateUpdated;
    public List<FileDTO> files;
}
