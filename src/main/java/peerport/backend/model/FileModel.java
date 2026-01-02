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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import peerport.backend.dto.FileDTO;

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

    private String url;

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
    @JoinColumn(name="\"files\"", nullable=false)
    private ContentModel content;


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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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


    // DTO Methods
    public FileDTO toDTO() {
        // Create a new DTO instance
        FileDTO dto = new FileDTO();

        // Fill in the fields
        dto.fileId = this.fileId;
        dto.fileName = this.fileName;
        dto.url = this.url;
        dto.contentType = this.contentType;
        dto.fileType = this.fileType;

        // Return the DTO
        return dto;
    }
}
