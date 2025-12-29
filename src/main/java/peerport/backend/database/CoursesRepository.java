package peerport.backend.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import peerport.backend.model.CourseModel;

@Repository
public interface CoursesRepository extends JpaRepository<CourseModel, String> {
}
