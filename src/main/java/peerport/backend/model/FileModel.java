package peerport.backend.model;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

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

    @CreatedDate
    @Column(name="\"dateCreated\"")
    private Date dateCreated;

    @Column(name="\"dateUpdated\"")
    private Date dateUpdated;

    // Connections
    @OneToOne(mappedBy="image")
    private CourseModel course;


    // Enviroment vairables
    @Value("${server.url}/files/:localhost:8080/files/")
    private static String fileUrl;


    // Default constructor
    public FileModel() { }

    // Parameterized constructor
    public FileModel(
        String fileName,
        String filePath,
        String fileType
    ) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
    }


    // Getters and Setters
    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
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

    public String getURL() {
        return fileUrl + this.fileId;
    }
}
