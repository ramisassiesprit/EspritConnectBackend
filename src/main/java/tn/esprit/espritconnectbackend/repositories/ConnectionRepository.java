package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.Connection;
import tn.esprit.espritconnectbackend.entities.User;
import java.util.List;
import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {
    List<Connection> findByRequesterOrAddressee(User requester, User addressee);
}
