package peerport.backend.model;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import peerport.backend.dto.AssignmentDTO;

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
    @JoinColumn(name="\"assignnments\"", nullable=false)
    private CourseModel course;


    // Default constructor
    public AssignmentModel() { }

    // Parameterized constructor
    public AssignmentModel(
        String assignmentId,
        String name,
        String description,
        Boolean visible,
        Date dueDate,
        Date dateCreated,
        Date dateUpdated,
        CourseModel course
    ) {
        this.assignmentId = assignmentId;
        this.name = name;
        this.description = description;
        this.visible = visible;
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


    // Conver to DTO
    public AssignmentDTO toDTO() {
        // Create a new DTO
        AssignmentDTO dto = new AssignmentDTO();

        // Fill in the fields
        dto.assignmentId = this.assignmentId;
        dto.name = this.name;
        dto.description = this.description;
        dto.visible = this.visible;
        dto.dueDate = this.dueDate;
        dto.courseId = this.course.getCourseId();
        dto.dateCreated = this.dateCreated;
        dto.dateUpdated = this.dateUpdated;

        // Return the dto
        return dto;
    }
}
