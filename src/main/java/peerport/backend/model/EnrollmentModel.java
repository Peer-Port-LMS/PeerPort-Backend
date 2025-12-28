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
import jakarta.validation.constraints.PastOrPresent;

@Entity
@Table(name="\"Enrollments\"")
@ValidEndDateAfterStartDate(startDate="dateEnrolled", endDate="dateCompleted")
public class EnrollmentModel {
    
    @Id
    @Column(name="\"enrollmentId\"")
    @GeneratedValue(strategy=GenerationType.UUID)
    private String enrollmentId;

    @Column(name="\"dateEnrolled\"", nullable=false, updatable=false)
    private final Date dateEnrolled = new Date();

    @Column(name="\"dateCompleted\"", nullable=true)
    @PastOrPresent(message="Completion date cannot be in the future")
    private Date dateCompleted;

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
        Date dateCompleted,
        UserModel user,
        CourseModel course
    ) {
        this.enrollmentId = enrollmentId;
        this.dateCompleted = dateCompleted;
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

    public Date getDateEnrolled() {
        return dateEnrolled;
    }

    public Date setDateEnrolled(Date dateEnrolled) {
        return this.dateEnrolled;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
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
