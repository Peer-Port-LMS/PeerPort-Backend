package peerport.backend.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="\"Enrollments\"")
public class EnrollmentModel {
    
    @Id
    @Column(name="\"enrollmentId\"")
    @GeneratedValue(strategy=GenerationType.UUID)
    private String enrollmentId;

    private Boolean enrolled;

    private Boolean completed;

    @Column(name="\"dateEnrolled\"")
    private Date dateEnrolled;

    // Connections
    @ManyToOne
    @JoinColumn(name="\"userId\"", nullable=false)
    private UserModel user;

    @ManyToOne
    @JoinColumn(name="\"courseId\"", nullable=false)
    private CourseModel course;


    // Default constructor
    public EnrollmentModel() { }

    // Parameterized constructor
    public EnrollmentModel(
        String enrollmentId,
        Boolean enrolled,
        Boolean completed,
        Date dateEnrolled,
        UserModel user,
        CourseModel course
    ) {
        this.enrollmentId = enrollmentId;
        this.enrolled = enrolled;
        this.completed = completed;
        this.dateEnrolled = dateEnrolled;
        this.user = user;
        this.course = course;
    }


    // Getters and Setters
    public String getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(String enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Boolean getEnrolled() {
        return enrolled;
    }

    public void setEnrolled(Boolean enrolled) {
        this.enrolled = enrolled;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Date getDateEnrolled() {
        return dateEnrolled;
    }

    public void setDateEnrolled(Date dateEnrolled) {
        this.dateEnrolled = dateEnrolled;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public CourseModel getCourse() {
        return course;
    }

    public void setCourse(CourseModel course) {
        this.course = course;
    }
}
