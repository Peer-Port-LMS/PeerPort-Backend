package peerport.backend.dto.courses;

import peerport.backend.dto.announcements.AnnouncementDTO;

public class CourseWithAnnouncementsDTO extends CourseDTO {
    public AnnouncementDTO[] announcements;
}
