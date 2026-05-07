package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.Event;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
}
