package peerport.backend.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import peerport.backend.model.EnrollmentModel;

@Repository
public interface EnrollmentsRepository extends JpaRepository<EnrollmentModel, String> {
}
