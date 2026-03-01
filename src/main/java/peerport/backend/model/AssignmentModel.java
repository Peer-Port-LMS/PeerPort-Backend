package peerport.backend.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import peerport.backend.dto.assignments.AssignmentDTO;
import peerport.backend.dto.assignments.AssignmentWithCourseDTO;

@Entity
@Table(name="\"Assignments\"")
public class AssignmentModel {
    
    @Id
    @Column(name="\"assignmentId\"")
    @GeneratedValue(strategy=GenerationType.UUID)
    private String assignmentId;

    @NotNull
    @NotBlank(message="Name cannot be blank")
    private String name;

    @Length(max=2000, message="Description cannot exceed 2000 characters")
    private String description;

    private Boolean visible = true;
    
    @NotNull
    @Column(name="\"gradesVisible\"")
    private Boolean gradesVisible = true;

    @NotNull
    @Column(name="\"dueDate\"")
    private Date dueDate;

    @CreationTimestamp
    @Column(name="\"dateCreated\"")
    private Date dateCreated;

    @UpdateTimestamp
    @Column(name="\"dateUpdated\"")
    private Date dateUpdated;

    // Connections
    @ManyToOne
    @JoinColumn(name="\"assignments\"", nullable=false)
    private CourseModel course;

    @OneToMany(mappedBy="assignment", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<FileModel> files = new ArrayList<>();

    @OneToMany(mappedBy="assignment", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<AssignmentSubmissionModel> submissions = new ArrayList<>();


    // Default constructor
    public AssignmentModel() { }

    // Parameterized constructor
    public AssignmentModel(
        String assignmentId,
        String name,
        String description,
        Boolean visible,
        Boolean gradesVisible,
        Date dueDate,
        Date dateCreated,
        Date dateUpdated,
        CourseModel course
    ) {
        this.assignmentId = assignmentId;
        this.name = name;
        this.description = description;
        this.visible = visible;
        this.gradesVisible = gradesVisible;
        this.dueDate = dueDate;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.course = course;
    }


    // Getters and Setters
    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Boolean getGradesVisible() {
        return gradesVisible;
    }

    public void setGradesVisible(Boolean gradesVisible) {
        this.gradesVisible = gradesVisible;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
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

    public List<FileModel> getFiles() {
        return files;
    }

    public void setFiles(List<FileModel> files) {
        this.files = files;
    }

    public List<AssignmentSubmissionModel> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<AssignmentSubmissionModel> submissions) {
        this.submissions = submissions;
    }


    // Convert to DTO
    public <T extends AssignmentDTO> T baseFill(T dto) {
        // Fill in the fields
        dto.assignmentId = this.assignmentId;
        dto.name = this.name;
        dto.description = this.description;
        dto.visible = this.visible;
        dto.gradesVisible = this.gradesVisible;
        dto.dueDate = this.dueDate;
        dto.courseId = this.course.getCourseId();
        dto.dateCreated = this.dateCreated;
        dto.dateUpdated = this.dateUpdated;

        dto.grade = -1; // Default value indicating no grade available
        
        // Return the dto
        return dto;
    }

    public AssignmentDTO toDTO() {
        // Create a new DTO
        AssignmentDTO dto = new AssignmentDTO();

        // Fill in the fields
        dto = baseFill(dto);
        
        // Add files
        dto.files = this.files != null 
            ? this.files.stream().map(FileModel::toDTO).toList() 
            : new ArrayList<>();

        // Return the dto
        return dto;
    }

    public AssignmentWithCourseDTO toAssignmentWithCourseDTO() {
        // Create new DTO
        AssignmentWithCourseDTO dto = new AssignmentWithCourseDTO();

        // Fill in the base fields
        dto = baseFill(dto);

        // Fill in the course field
        dto.course = this.course.toDTO();

        // Return the dto
        return dto;
    }
}
