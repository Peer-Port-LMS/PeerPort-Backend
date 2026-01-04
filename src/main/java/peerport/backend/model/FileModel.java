package peerport.backend.model;

import java.io.IOException;
import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import peerport.backend.dto.FileDTO;
import peerport.backend.service.FileService;

@Entity
@Table(name="\"Files\"")
public class FileModel {
    @Id
    @Column(name="\"fileId\"")
    @GeneratedValue(strategy=GenerationType.UUID)
    private String fileId;

    @Column(name="\"fileName\"", nullable=false, updatable=false)
    private String fileName;

    @Column(name="\"filePath\"", nullable=false, updatable=false)
    private String filePath;

    @Column(name="\"fileType\"", nullable=false)
    private String fileType;

    @Column(name="\"contentType\"", nullable=false)
    private String contentType;

    @CreationTimestamp
    @Column(name="\"dateCreated\"")
    private Date dateCreated = new Date();

    @UpdateTimestamp
    @Column(name="\"dateUpdated\"")
    private Date dateUpdated;


    // Connections
    @OneToOne(mappedBy="image", orphanRemoval=true)
    private CourseModel course;

    @ManyToOne
    @JoinColumn(name="\"contentId\"")
    private ContentModel content;

    @ManyToOne
    @JoinColumn(name="\"announcementId\"")
    private AnnouncementModel announcement;


    // Environment variables (ignored by JPA)
    @Transient
    @Value("${server.hosting-url}")
    private String serverUrl;

    @Transient
    private static String filesEndpoint = "files";

    @Transient
    @Autowired
    private FileService fileService;

    // Default constructor
    public FileModel() { }

    // Parameterized constructor
    public FileModel(
        String fileName,
        String filePath,
        String fileType,
        String contentType
    ) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.contentType = contentType;
    }


    // Getters and Setters
    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public CourseModel getCourse() {
        return course;
    }

    public void setCourse(CourseModel course) {
        this.course = course;
    }

    public ContentModel getContent() {
        return content;
    }

    public void setContent(ContentModel content) {
        this.content = content;
    }

    public AnnouncementModel getAnnouncement() {
        return announcement;
    }

    public void setAnnouncement(AnnouncementModel announcement) {
        this.announcement = announcement;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }


    public String getUrl() {
        return serverUrl + "/" + filesEndpoint + "/" + fileId;
    }


    // DTO Methods
    public FileDTO toDTO() {
        // Create a new DTO instance
        FileDTO dto = new FileDTO();

        // Fill in the fields
        dto.fileId = this.fileId;
        dto.fileName = this.fileName;
        dto.url = this.serverUrl + "/" + FileModel.filesEndpoint + "/" + this.fileId;
        dto.contentType = this.contentType;
        dto.fileType = this.fileType;

        // Return the DTO
        return dto;
    }


    // Pre removal hook
    @PreRemove
    public void deletePhysicalFile() {
        try {
            // Use FileService to delete the physical file
            fileService.deleteFile(this);

        // Catch any IO exceptions
        } catch (IOException e) {
            // Log the error
            System.err.println("Failed to delete physical file: " + e.getMessage());
        }
    }
}
