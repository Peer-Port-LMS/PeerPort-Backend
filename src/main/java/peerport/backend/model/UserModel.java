package peerport.backend.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import peerport.backend.dto.UserDTO;

@Entity
@Table(name="\"Users\"")
public class UserModel {
    
    @Id
    @Column(name="\"userId\"")
    @GeneratedValue(strategy=GenerationType.UUID)
    private String userId;

    @Size(min=2, message="Name must be at least 2 characters long")
    private String name;

    @Email(message="Email should be valid")
    private String email;

    @Column(name="\"profilePictureUrl\"")
    private String profilePictureUrl;

    @Column(name="\"idNumber\"")
    private String idNumber;

    // Incase the role isn't set, default to STUDENT
    private Enum<RoleModel.Role> role = RoleModel.Role.STUDENT;

    // Oauth2 fields
    private String provider;

    @Column(name="\"providerId\"")
    private String providerId;

    // Connections
    @OneToMany(mappedBy="user", cascade=CascadeType.ALL)
    private List<EnrollmentModel> enrollments;

    @ManyToMany(mappedBy="instructors")
    private List<CourseModel> instructedCourses = new ArrayList<>();


    // Default constructor
    public UserModel() { }


    // Parameterized constructor
    public UserModel(
        String userId, 
        String name, 
        String email,
        String profilePictureUrl,
        String idNumber,
        Enum<RoleModel.Role> role
    ) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.idNumber = idNumber;
        this.role = role;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public Enum<RoleModel.Role> getRole() {
        return role;
    }

    public void setRole(Enum<RoleModel.Role> role) {
        this.role = role;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean addEnrollment(EnrollmentModel enrollment) {
        return this.enrollments.add(enrollment);
    }

    public List<EnrollmentModel> getEnrollments() {
        return enrollments;
    }

    public List<CourseModel> getTaughtCourses() {
        return instructedCourses;
    }

    public boolean addTaughtCourse(CourseModel course) {
        if (this.role == RoleModel.Role.INSTRUCTOR || this.role == RoleModel.Role.ADMIN) {
            this.instructedCourses.add(course);
            return true;
        }
        return false;
    }


    // Conversions
    public UserDTO toDTO() {
        UserDTO dto = new UserDTO();
        dto.userId = this.userId;
        dto.name = this.name;
        dto.email = this.email;
        dto.profilePictureUrl = this.profilePictureUrl;
        dto.idNumber = this.idNumber;
        dto.role = this.role;
        return dto;
    }
}

