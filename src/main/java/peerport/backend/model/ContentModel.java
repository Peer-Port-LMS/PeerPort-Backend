package peerport.backend.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import peerport.backend.dto.ContentDTO;
import peerport.backend.dto.ContentWithAllDetailsDTO;
import peerport.backend.dto.ContentWithChildrenDTO;
import peerport.backend.dto.ContentWithCourseDTO;

@Entity
@Table(name="\"Content\"")
public class ContentModel {

    @Id
    @Column(
        name="\"contentId\"", 
        insertable=false, 
        updatable=false
    )
    @GeneratedValue(strategy=GenerationType.UUID)
    private String contentId;

    private String title;

    private String description;

    private Boolean visible;

    @Column(name="\"dateCreated\"")
    private Date dateCreated;

    @Column(name="\"dateUpdated\"")
    private Date dateUpdated;

    // Connections
    @ManyToOne
    @JoinColumn(name="\"courseId\"", nullable=false)
    private CourseModel course;

    @ManyToOne
    @JoinColumn(name="\"parentContentId\"", nullable=true)
    private ContentModel parentContent;

    @OneToMany(mappedBy="parentContent")
    private List<ContentModel> subContent;

    @OneToMany(mappedBy="content", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<FileModel> files;


    // Default constructor
    public ContentModel() { }

    // Parameterized constructor
    public ContentModel(
        String contentId,
        String title,
        String description,
        Boolean visible,
        Date dateCreated,
        Date dateUpdated,
        CourseModel course,
        ContentModel parentContent,
        List<ContentModel> subContent,
        List<FileModel> files
    ) {
        this.contentId = contentId;
        this.title = title;
        this.description = description;
        this.visible = visible;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.course = course;
        this.parentContent = parentContent;
        this.subContent = subContent;
    }


    // Getters and Setters
    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public CourseModel getCourse() {
        return course;
    }

    public void setCourse(CourseModel course) {
        this.course = course;
    }

    public ContentModel getParentContent() {
        return parentContent;
    }

    public void setParentContent(ContentModel parentContent) {
        this.parentContent = parentContent;
    }

    public List<ContentModel> getSubContent() {
        return subContent;
    }

    public void setSubContent(List<ContentModel> subContent) {
        this.subContent = subContent;
    }

    public List<FileModel> getFiles() {
        return files;
    }

    public void setFiles(List<FileModel> files) {
        this.files = files;
    }



    // To DTO methods
    public <T extends ContentDTO> T fillInDTO(T dto) {
        // Fill in common fields
        dto.contentId = this.contentId;
        dto.title = this.title;
        dto.description = this.description;
        dto.visible = this.visible;
        dto.dateCreated = this.dateCreated;
        dto.dateUpdated = this.dateUpdated;
        dto.files = this.files.stream()
            .map(FileModel::toDTO)
            .toList();

        // Return the filled DTO
        return dto;
    }

    public ContentDTO toDTO() {
        return fillInDTO(new ContentDTO());
    }

    public ContentWithCourseDTO toContentWithCourseDTO() {
        // Fill in general fields
        ContentWithCourseDTO dto = fillInDTO(new ContentWithCourseDTO());

        // Set the course field
        dto.course = this.course.toDTO();

        // Return the filled DTO
        return dto;
    }

    public ContentWithChildrenDTO toContentWithChildrenDTO() {
        // Fill in general fields
        ContentWithChildrenDTO dto = fillInDTO(new ContentWithChildrenDTO());

        // Set the parentContent field
        if (this.parentContent != null) {
            dto.parentContent = this.parentContent.toDTO();
        }

        // Set the subContent field
        dto.subContent = this.subContent.stream()
            .map(ContentModel::toContentWithChildrenDTO)
            .toList();

        // Return the filled DTO
        return dto;
    }

    public ContentWithAllDetailsDTO toContentWithAllDetailsDTO() {
        // Fill in general fields
        ContentWithAllDetailsDTO dto = fillInDTO(new ContentWithAllDetailsDTO());

        // Set the course field
        dto.course = this.course.toDTO();

        // Set the subContent field
        dto.subContent = this.subContent.stream()
            .map(ContentModel::toDTO)
            .toList();

        // Return the filled DTO
        return dto;
    }
}
