package peerport.backend.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import peerport.backend.model.AnnouncementModel;

@Repository
public interface AnnouncementsRepository extends JpaRepository<AnnouncementModel, String> {
}
