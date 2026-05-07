package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.espritconnectbackend.entities.EspritProfile;
import java.util.UUID;

public interface EspritProfileRepository extends JpaRepository<EspritProfile, UUID> {
}
