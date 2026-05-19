package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.Group;
import tn.esprit.espritconnectbackend.entities.GroupMemberCriteria;

import java.util.Optional;
import java.util.UUID;

public interface GroupMemberCriteriaRepository extends JpaRepository<GroupMemberCriteria, UUID> {
    Optional<GroupMemberCriteria> findByGroup(Group group);
}
