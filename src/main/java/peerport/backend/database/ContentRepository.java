package peerport.backend.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import peerport.backend.model.ContentModel;

@Repository
public interface ContentRepository extends JpaRepository<ContentModel, String> {
}
