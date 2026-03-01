package peerport.backend.model;

import java.util.Date;
import java.util.Optional;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import peerport.backend.dto.GradeDTO;

@Entity
@Table(name="\"Grades\"")
public class GradeModel {
    @Id
    @Column(name="\"gradeId\"")
    @GeneratedValue(strategy=GenerationType.UUID)
    private String gradeId;

    @Column(name="\"gradeObtained\"")
    @NotNull(message="Grade value is required")
    private int gradeObtained = -1;

    @Column(name="\"maxGrade\"")
    @NotNull(message="Max grade is required")
    private int maxGrade;

    @Column(name="\"feedback\"")
    @Size(max=500, message="Feedback cannot exceed 500 characters")
    private String feedback;


    @CreationTimestamp
    @Column(name="\"dateGraded\"")
    private Date dateGraded;

    @UpdateTimestamp
    @Column(name="\"dateUpdated\"")
    private Date dateUpdated;


    // Connections
    @OneToOne
    private AssignmentSubmissionModel submission;

    // Constructors
    public GradeModel() {}

    public GradeModel(String gradeId, int gradeObtained, int maxGrade, String feedback) {
        this.gradeId = gradeId;
        this.gradeObtained = gradeObtained;
        this.maxGrade = maxGrade;
        this.feedback = feedback;
    }

    // Getters and Setters
    public String getGradeId() {
        return gradeId;
    }

    public int getGradeObtained() {
        return gradeObtained;
    }

    public void setGradeObtained(int gradeObtained) {
        this.gradeObtained = gradeObtained;
    }

    public int getMaxGrade() {
        return maxGrade;
    }

    public void setMaxGrade(int maxGrade) {
        this.maxGrade = maxGrade;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Date getDateGraded() {
        return dateGraded;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    public AssignmentSubmissionModel getSubmission() {
        return submission;
    }

    public void setSubmission(AssignmentSubmissionModel submission) {
        this.submission = submission;
    }

    public float getPercentage() {
        if (this.gradeObtained == -1) {
            return -1;
        }
        return ((float)this.gradeObtained / this.maxGrade) * 100;
    }


    // DTO methods
    public GradeDTO toDTO() {
        // Create the DTO
        GradeDTO dto = new GradeDTO();

        // Fill in the DTO
        dto.gradeId = this.gradeId;
        dto.maxGrade = this.maxGrade;
        dto.dateGraded = this.dateGraded;
        dto.dateUpdated = this.dateUpdated;

        // Check the value of gradeObtained
        if (this.gradeObtained == -1) {
            dto.gradeObtained = Optional.empty();
        } else {
            dto.gradeObtained = Optional.of(this.gradeObtained);
        }

        // Return the DTO
        return dto;
    }
}
