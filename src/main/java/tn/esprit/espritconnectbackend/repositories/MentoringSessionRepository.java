package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.espritconnectbackend.entities.MentoringRequest;
import tn.esprit.espritconnectbackend.entities.MentoringSession;
import java.util.List;
import java.util.UUID;

public interface MentoringSessionRepository extends JpaRepository<MentoringSession, UUID> {
    List<MentoringSession> findByRequest(MentoringRequest request);

    @Query("SELECT AVG(s.rating) FROM MentoringSession s WHERE s.rating IS NOT NULL")
    Double averageRating();
}
