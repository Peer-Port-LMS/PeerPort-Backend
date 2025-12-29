package peerport.backend.model;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import peerport.backend.dto.AnnouncementDTO;

@Entity
@Table(name="\"Announcements\"")
public class AnnouncementModel {

    @Id
    @Column(name="\"announcementId\"", updatable=false, nullable=false)
    @GeneratedValue(strategy=GenerationType.UUID)
    private String announcementId;

    @NotBlank(message="Title cannot be blank")
    public String title;

    @NotBlank(message="Content cannot be blank")
    public String content;

    @CreationTimestamp
    @Column(name="\"dateCreated\"", updatable=false, nullable=false)
    private Date dateCreated;

    @UpdateTimestamp
    @Column(name="\"dateUpdated\"", nullable=false)
    private Date dateUpdated;

    // Connections
    @ManyToOne
    @JoinColumn(name="\"courseId\"", nullable=false)
    private CourseModel course;


    // Default constructor
    public AnnouncementModel() { }

    // Parameterized constructor
    public AnnouncementModel(
        String announcementId,
        String title,
        String content,
        CourseModel course
    ) {
        this.announcementId = announcementId;
        this.title = title;
        this.content = content;
        this.course = course;
    }

    // Getters and Setters
    public String getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(String announcementId) {
        this.announcementId = announcementId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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


    // To DTO
    public AnnouncementDTO toDTO() {
        // Create new DTO
        AnnouncementDTO dto = new AnnouncementDTO();
        
        // Fill in fields
        dto.announcementId = this.announcementId;
        dto.title = this.title;
        dto.content = this.content;
        dto.dateCreated = this.dateCreated;
        dto.dateUpdated = this.dateUpdated;
        
        // Return DTO
        return dto;
    }
}
