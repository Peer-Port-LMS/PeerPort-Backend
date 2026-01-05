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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import peerport.backend.dto.UserDTO;
import peerport.backend.dto.announcements.AnnouncementDTO;
import peerport.backend.dto.assignments.AssignmentDTO;
import peerport.backend.dto.courses.CourseDTO;
import peerport.backend.dto.courses.CourseWithAllDetailsDTO;
import peerport.backend.dto.courses.CourseWithAnnouncementsDTO;
import peerport.backend.dto.courses.CourseWithInstructorsDTO;
import peerport.backend.model.groups.OnCreate;
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
    @FutureOrPresent(message="Start date cannot be in the past", groups=OnCreate.class)
    private Date startDate;

    @Column(name="\"endDate\"")
    @FutureOrPresent(message="End date cannot be in the past", groups=OnCreate.class)
    private Date endDate;

    // Connections
    @OneToMany(mappedBy="course", cascade=CascadeType.ALL)
    private List<EnrollmentModel> enrollments;

    @ManyToMany
    @JoinTable(
        name="\"CourseInstructors\"",
        joinColumns=@JoinColumn(name="\"courseId\""),
        inverseJoinColumns=@JoinColumn(name="\"userId\"")
    )
    private List<UserModel> instructors = new ArrayList<>();

    @OneToMany(mappedBy="course", cascade=CascadeType.ALL)
    private List<AnnouncementModel> announcements = new ArrayList<>();

    @OneToMany(mappedBy="course", cascade=CascadeType.ALL)
    private List<AssignmentModel> assignments = new ArrayList<AssignmentModel>();

    @OneToMany(mappedBy="course", cascade=CascadeType.ALL)
    private List<ContentModel> content = new ArrayList<>();

    @OneToOne(orphanRemoval=true)
    private FileModel image;

    // Default constructor
    public CourseModel() { }


    // Parameterized constructor
    public CourseModel(
        String id,
        String name, 
        String courseCode,
        Boolean isOpen,
        String description,
        Date startDate,
        Date endDate
    ) {
        this.courseId = id;
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
        return this.instructors.remove(instructor);
    }

    public boolean addAnnouncement(AnnouncementModel announcement) {
        return this.announcements.add(announcement);
    }

    public List<AnnouncementModel> getAnnouncements() {
        return announcements;
    }

    public boolean addAssignment(AssignmentModel assignment) {
        return this.assignments.add(assignment);
    }

    public List<AssignmentModel> getAssignments() {
        return assignments;
    }

    public void addContent(ContentModel contentItem) {
        this.content.add(contentItem);
    }

    public List<ContentModel> getContent() {
        return content;
    }

    public FileModel getImage() {
        return image;
    }

    public void setImage(FileModel image) {
        this.image = image;
    }

    public List<UserModel> getUsers() {
        List<UserModel> users = new ArrayList<>();
        
        // Get users from enrollments
        for (EnrollmentModel enrollment : this.enrollments) {
            users.add(enrollment.getUser());
        }

        // Get users from instructors
        users.addAll(this.instructors);

        // Return the list
        return users;
    }


    // DTO conversion helper
    private <T extends CourseDTO> T fillInBasicInfo(T dto) {
        // Fill in all standard fields
        dto.courseId = this.courseId;
        dto.name = this.name;
        dto.courseCode = this.courseCode;
        dto.isOpen = this.isOpen;
        dto.description = this.description;
        dto.startDate = this.startDate != null ? this.startDate.toString() : null;
        dto.endDate = this.endDate != null ? this.endDate.toString() : null;
        dto.visible = this.isOpen;
        dto.imageURL = this.image != null ? this.image.getUrl() : null;

        // Return filled in dto
        return dto;
    }

    // Convert to DTO
    public CourseDTO toDTO() {
        // Make new DTO
        CourseDTO dto = new CourseDTO();

        // Return the filled in DTO
        return fillInBasicInfo(dto);
    }

    public CourseWithInstructorsDTO toCourseWithInstructorsDTO() {
        // Make new DTO 
        CourseWithInstructorsDTO dto = new CourseWithInstructorsDTO();

        // Populate fields
        dto = fillInBasicInfo(dto);

        // Convert instructors to DTOs then to an array
        dto.instructors = this.instructors.stream()
                .map(UserModel::toDTO)
                .toArray(UserDTO[]::new);
        
        // Return the DTO
        return dto;
    }

    public CourseWithAnnouncementsDTO toCourseWithAnnouncementsDTO() {
        // Make new DTO 
        CourseWithAnnouncementsDTO dto = new CourseWithAnnouncementsDTO();

        // Fill in basic fields
        dto = fillInBasicInfo(dto);

        // Convert announcements to DTOs then to an array
        dto.announcements = this.announcements.stream()
                .map(AnnouncementModel::toDTO)
                .toArray(AnnouncementDTO[]::new);
        
        // Return the DTO
        return dto;
    }

    public CourseWithAllDetailsDTO toCourseWithAllDetailsDTO() {
        // Make new DTO 
        CourseWithAllDetailsDTO dto = new CourseWithAllDetailsDTO();

        // Fill in basic fields
        dto = fillInBasicInfo(dto);

        // Convert instructors to DTOs then to an array
        dto.instructors = this.instructors.stream()
                .map(UserModel::toDTO)
                .toArray(UserDTO[]::new);

        // Convert announcements to DTOs then to an array
        // Then sort by dateUpdated descending
        dto.announcements = this.announcements.stream()
                .map(AnnouncementModel::toDTO)
                .sorted((a1, a2) -> a2.dateUpdated.compareTo(a1.dateUpdated))
                .toArray(AnnouncementDTO[]::new);

        // Convert assignments to DTOs then to an array
        // Then sort by dueDate ascending
        dto.assignments = this.assignments.stream()
                .map(AssignmentModel::toDTO)
                .sorted((a1, a2) -> {
                    if (a1.dueDate == null && a2.dueDate == null) return 0;
                    if (a1.dueDate == null) return 1;
                    if (a2.dueDate == null) return -1;
                    return a1.dueDate.compareTo(a2.dueDate);
                })
                .toArray(AssignmentDTO[]::new);
        
        // Return the DTO
        return dto;
    }
}
