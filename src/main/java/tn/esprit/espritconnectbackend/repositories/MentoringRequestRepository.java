package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
