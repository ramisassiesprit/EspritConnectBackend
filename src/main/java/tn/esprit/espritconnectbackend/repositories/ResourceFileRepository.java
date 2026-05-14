package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.ResourceFile;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceFileRepository extends JpaRepository<ResourceFile, UUID> {
    List<ResourceFile> findByFolderId(UUID folderId);
}
