package peerport.backend.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
    @JoinColumn(name="\"contentId\"", nullable=true)
    private ContentModel parentContent;


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
        ContentModel parentContent
    ) {
        this.contentId = contentId;
        this.title = title;
        this.description = description;
        this.visible = visible;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.course = course;
        this.parentContent = parentContent;
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
}
