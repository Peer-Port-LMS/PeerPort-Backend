package peerport.backend.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


@Entity
@Table(name="\"Courses\"")
public class CourseModel {

    @Id
    @Column(name="\"courseId\"")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String courseId;

    private String name;

    @Column(name="\"courseCode\"")
    private String courseCode;

    @Column(name="\"isOpen\"")
    private Boolean isOpen;

    private String description;

    @Column(name="\"startDate\"")
    private Date startDate;

    @Column(name="\"endDate\"")
    private Date endDate;

    @OneToMany(mappedBy="course", cascade=CascadeType.ALL)
    private List<EnrollmentModel> enrollments;


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


    @Override
    public String toString() {
        return "CourseModel{" +
                "courseId='" + courseId + '\'' +
                ", name='" + name + '\'' +
                ", courseCode='" + courseCode + '\'' +
                ", isOpen=" + isOpen +
                ", description='" + description + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
