package peerport.backend.dto.content;

import java.util.List;

public class ContentWithChildrenDTO extends ContentDTO {
    public ContentDTO parentContent;
    public List<ContentWithChildrenDTO> subContent;
}
