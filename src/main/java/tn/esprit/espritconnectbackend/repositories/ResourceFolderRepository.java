package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.ResourceFolder;

import java.util.UUID;

@Repository
public interface ResourceFolderRepository extends JpaRepository<ResourceFolder, UUID> {
}
