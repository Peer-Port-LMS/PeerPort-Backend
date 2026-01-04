package peerport.backend.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import peerport.backend.model.AssignmentSubmissionModel;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmissionModel, String>{}