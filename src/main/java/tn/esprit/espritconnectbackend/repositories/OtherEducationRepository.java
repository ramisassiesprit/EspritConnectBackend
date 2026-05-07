package tn.esprit.espritconnectbackend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.esprit.espritconnectbackend.entities.OtherEducation;
import tn.esprit.espritconnectbackend.entities.User;

@Repository
public interface OtherEducationRepository extends JpaRepository<OtherEducation, UUID> {
    List<OtherEducation> findByUserId(UUID userId);
    List<OtherEducation> findByUser(User user);
}
