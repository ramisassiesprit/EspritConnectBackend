package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.Group;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
}
