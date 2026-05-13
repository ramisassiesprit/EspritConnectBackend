package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.WillingToHelp;
import tn.esprit.espritconnectbackend.entities.User;
import java.util.List;
import java.util.UUID;

public interface WillingToHelpRepository extends JpaRepository<WillingToHelp, UUID> {
    List<WillingToHelp> findByUser(User user);
    List<WillingToHelp> findByUserId(UUID userId);
}
