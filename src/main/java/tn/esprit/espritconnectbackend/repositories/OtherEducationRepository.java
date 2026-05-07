package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.OtherEducation;
import tn.esprit.espritconnectbackend.entities.User;
import java.util.List;
import java.util.UUID;

public interface OtherEducationRepository extends JpaRepository<OtherEducation, UUID> {
    List<OtherEducation> findByUser(User user);
}
