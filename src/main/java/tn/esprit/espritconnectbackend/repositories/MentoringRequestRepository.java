package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.espritconnectbackend.entities.MentoringRequest;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.MentoringStatus;

import java.util.List;
import java.util.UUID;

public interface MentoringRequestRepository extends JpaRepository<MentoringRequest, UUID> {
    List<MentoringRequest> findByMentor(User mentor);
    List<MentoringRequest> findByMentee(User mentee);
    List<MentoringRequest> findByMentorAndStatusIn(User mentor, List<MentoringStatus> statuses);
    List<MentoringRequest> findByMenteeAndStatusIn(User mentee, List<MentoringStatus> statuses);
    boolean existsByMentorAndStatus(User mentor, MentoringStatus status);
    boolean existsByMenteeAndStatus(User mentee, MentoringStatus status);
    boolean existsByMentorAndStatusAndIdNot(User mentor, MentoringStatus status, UUID id);
    boolean existsByMenteeAndStatusAndIdNot(User mentee, MentoringStatus status, UUID id);

    long countByStatus(MentoringStatus status);

    @Query("SELECT r.status, COUNT(r) FROM MentoringRequest r GROUP BY r.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT ep.fieldOfStudy, COUNT(r) FROM MentoringRequest r JOIN r.mentee.espritProfile ep WHERE ep.fieldOfStudy IS NOT NULL AND ep.fieldOfStudy <> '' GROUP BY ep.fieldOfStudy ORDER BY COUNT(r) DESC")
    List<Object[]> countByMenteeFieldOfStudy();
}
