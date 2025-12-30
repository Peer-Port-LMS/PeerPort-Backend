package peerport.backend.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import peerport.backend.model.FileModel;

@Repository
public interface FilesRepository extends JpaRepository<FileModel, String> {}