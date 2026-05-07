package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.WorkExperience;
import tn.esprit.espritconnectbackend.entities.User;
import java.util.List;
import java.util.UUID;

public interface WorkExperienceRepository extends JpaRepository<WorkExperience, UUID> {
    List<WorkExperience> findByUser(User user);
}
