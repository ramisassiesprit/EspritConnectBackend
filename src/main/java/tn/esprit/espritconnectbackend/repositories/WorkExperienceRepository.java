package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.WorkExperience;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkExperienceRepository extends JpaRepository<WorkExperience, UUID> {
    List<WorkExperience> findByUserId(UUID userId);
}
