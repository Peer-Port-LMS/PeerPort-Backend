package peerport.backend.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import peerport.backend.dto.FileDTO;
import peerport.backend.dto.assignments.AssignmentSubmissionDTO;
import peerport.backend.dto.assignments.AssignmentSubmissionWithDetailsDTO;

@Entity
@Table(name="\"AssignmentSubmissions\"")
public class AssignmentSubmissionModel {
    
    @Id
    @Column(name="\"assignmentSubmissionId\"")
    @GeneratedValue(strategy=GenerationType.UUID)
    private String assignmentSubmissionId;

    @CreationTimestamp
    @Column(name="\"dateSubmitted\"")
    private Date dateSubmitted;


    // Connections
    @OneToOne
    private UserModel user;

    @OneToOne
    private AssignmentModel assignment;

    @OneToMany(orphanRemoval=true)
    private List<FileModel> submittedFiles = new ArrayList<>();

    // @OneToOne
    // private GradeModel grade;


    // Constructors
    public AssignmentSubmissionModel() {}

    public AssignmentSubmissionModel(
        String assignmentSubmissionId, 
        Date dateSubmitted, 
        UserModel user,
        AssignmentModel assignment, 
        List<FileModel> submittedFiles
    ) {
        this.assignmentSubmissionId = assignmentSubmissionId;
        this.dateSubmitted = dateSubmitted;
        this.user = user;
        this.assignment = assignment;
        this.submittedFiles = submittedFiles;
    }


    // Getters and setters
    public String getAssignmentSubmissionId() {
        return assignmentSubmissionId;
    }

    public Date getDateSubmitted() {
        return dateSubmitted;
    }

    public void setDateSubmitted(Date dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public AssignmentModel getAssignment() {
        return assignment;
    }

    public void setAssignment(AssignmentModel assignment) {
        this.assignment = assignment;
    }

    public List<FileModel> getSubmittedFiles() {
        return submittedFiles;
    }

    public void setSubmittedFiles(List<FileModel> submittedFiles) {
        this.submittedFiles = submittedFiles;
    }


    // DTO methods
    private <T extends AssignmentSubmissionDTO> T fillDTO(T dto) {
        // Fill in the fields
        dto.assignmentSubmissionId = this.assignmentSubmissionId;
        dto.dateSubmitted = this.dateSubmitted;

        // Return the dto
        return dto;
    }

    public AssignmentSubmissionDTO toDTO() {
        // Make a new DTO
        AssignmentSubmissionDTO dto = new AssignmentSubmissionDTO();

        // Return the dto
        return fillDTO(dto);
    }

    public AssignmentSubmissionWithDetailsDTO toWithDetailsDTO() {
        // Create the DTO
        AssignmentSubmissionWithDetailsDTO dto = new AssignmentSubmissionWithDetailsDTO();
        
        // Fill in the fields
        dto.user = this.user.toDTO();
        dto.assignment = this.assignment.toDTO();
        dto.submittedFiles = new ArrayList<FileDTO>();
        for (FileModel file : this.submittedFiles) {
            dto.submittedFiles.add(file.toDTO());
        }

        // Return the dto
        return fillDTO(dto);
    }
}
