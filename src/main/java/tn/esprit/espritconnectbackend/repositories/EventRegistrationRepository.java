package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.Event;
import tn.esprit.espritconnectbackend.entities.EventRegistration;
import tn.esprit.espritconnectbackend.entities.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {
    List<EventRegistration> findByEvent(Event event);
    List<EventRegistration> findByUser(User user);
    Optional<EventRegistration> findByEventAndUser(Event event, User user);
    boolean existsByEventAndUser(Event event, User user);
    List<EventRegistration> findByEventAndStatusOrderByRegisteredAtAsc(Event event, tn.esprit.espritconnectbackend.entities.enums.RegistrationStatus status);
    
    long countByIsWinnerTrue();
    long countByCheckedInAtIsNotNull();
    
    @org.springframework.data.jpa.repository.Query("SELECT AVG(er.feedbackRating) FROM EventRegistration er WHERE er.feedbackRating IS NOT NULL")
    Double getAverageFeedbackScore();
}
