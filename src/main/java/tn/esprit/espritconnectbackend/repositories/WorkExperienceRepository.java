package tn.esprit.espritconnectbackend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.WorkExperience;

@Repository
public interface WorkExperienceRepository extends JpaRepository<WorkExperience, UUID> {
    List<WorkExperience> findByUserId(UUID userId);
    List<WorkExperience> findByUser(User user);
}
