package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.MentoringRequest;
import tn.esprit.espritconnectbackend.entities.MentoringSession;
import java.util.List;
import java.util.UUID;

public interface MentoringSessionRepository extends JpaRepository<MentoringSession, UUID> {
    List<MentoringSession> findByRequest(MentoringRequest request);
}
