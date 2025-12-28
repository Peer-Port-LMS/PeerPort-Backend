package peerport.backend.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import peerport.backend.dto.CourseDTO;
import peerport.backend.dto.UserDTO;
import peerport.backend.validation.ValidEndDateAfterStartDate;


@Entity
@Table(name="\"Courses\"")
@ValidEndDateAfterStartDate
public class CourseModel {

    @Id
    @Column(name="\"courseId\"")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String courseId;

    @NotBlank(message="Course name is required")
    @Size(min=2, message="Course name must be at least 2 characters long")
    private String name;

    @Column(name="\"courseCode\"")
    @Size(min=2, message="Course code must be at least 2 characters long")
    private String courseCode;

    @Column(name="\"isOpen\"")
    private Boolean isOpen = false;

    @Size(max=500, message="Description cannot exceed 500 characters")
    private String description;

    @Column(name="\"startDate\"")
    @NotNull(message="Start date is required")
    @FutureOrPresent(message="Start date cannot be in the past")
    private Date startDate;

    @Column(name="\"endDate\"")
    @FutureOrPresent(message="End date cannot be in the past")
    private Date endDate;

    // Connections
    @OneToMany(mappedBy="course", cascade=CascadeType.ALL)
    private List<EnrollmentModel> enrollments;

    @ManyToMany
    @JoinTable(
        name="\"CourseInstructors\"",
        joinColumns={@JoinColumn(name = "\"courseId\"")},
        inverseJoinColumns={@JoinColumn(name = "\"userId\"")}  
    )
    private List<UserModel> instructors = new ArrayList<>();

    // Default constructor
    public CourseModel() { }


    // Parameterized constructor
    public CourseModel(
        String courseId, 
        String name, 
        String courseCode,
        Boolean isOpen,
        String description,
        Date startDate,
        Date endDate
    ) {
        this.courseId = courseId;
        this.name = name;
        this.courseCode = courseCode;
        this.isOpen = isOpen;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }


    // Getters and Setters
    public String getCourseId() {
        return courseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean isOpen) {
        this.isOpen = isOpen;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public List<UserModel> getInstructors() {
        return this.instructors;
    }

    public boolean addInstructor(UserModel instructor) {
        return this.instructors.add(instructor);
    }

    public boolean removeInstructor(UserModel instructor) {
        if (this.instructors.remove(instructor)) {
            return true;
        }
        return false;
    }


    // Convert to DTO
    public CourseDTO toDTO() {
        // Make new DTO 
        CourseDTO dto = new CourseDTO();

        // Populate fields
        dto.courseId = this.courseId;
        dto.name = this.name;
        dto.courseCode = this.courseCode;
        dto.isOpen = this.isOpen;
        dto.description = this.description;
        dto.startDate = this.startDate != null ? this.startDate.toString() : null;
        dto.endDate = this.endDate != null ? this.endDate.toString() : null;
        dto.visiable = this.isOpen;

        // Convert instructors to DTOs then to an array
        dto.instructors = this.instructors.stream()
                .map(UserModel::toDTO)
                .toArray(UserDTO[]::new);
        
        // Return the DTO
        return dto;
    }
}
