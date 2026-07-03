package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.espritconnectbackend.entities.Event;
import tn.esprit.espritconnectbackend.entities.enums.EventStatus;
import tn.esprit.espritconnectbackend.entities.enums.EventType;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByGroupId(UUID groupId);
    List<Event> findByStatusIn(List<EventStatus> statuses);
    List<Event> findTop3ByOrderByRegisteredCountDesc();
    // Statistics queries
    @Query("SELECT COUNT(e) FROM Event e")
    long countTotalEvents();

    @Query("SELECT SUM(e.registeredCount) FROM Event e")
    Long countTotalParticipants();

    @Query("SELECT COUNT(DISTINCT e.status) as status, COUNT(e) as count FROM Event e GROUP BY e.status")
    List<Object[]> countEventsByStatus();

    @Query("SELECT COUNT(DISTINCT e.eventType) as eventType, COUNT(e) as count FROM Event e GROUP BY e.eventType")
    List<Object[]> countEventsByType();

    long countByStatus(EventStatus status);
    long countByEventType(EventType eventType);

    @Query(value = "SELECT DATE_FORMAT(e.created_at, '%Y-%m') as month, COUNT(er.id) as count " +
            "FROM event e LEFT JOIN event_registration er ON er.event_id = e.id " +
            "GROUP BY DATE_FORMAT(e.created_at, '%Y-%m') " +
            "ORDER BY DATE_FORMAT(e.created_at, '%Y-%m') ASC",
            nativeQuery = true)
    List<Object[]> countRegistrationsByMonth();
}
