package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.MentoringRequest;
import tn.esprit.espritconnectbackend.entities.User;
import java.util.List;
import java.util.UUID;

public interface MentoringRequestRepository extends JpaRepository<MentoringRequest, UUID> {
    List<MentoringRequest> findByMentor(User mentor);
    List<MentoringRequest> findByMentee(User mentee);
}
