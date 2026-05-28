package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.espritconnectbackend.entities.EspritProfile;
import java.util.List;
import java.util.UUID;

public interface EspritProfileRepository extends JpaRepository<EspritProfile, UUID> {
    @Query("SELECT DISTINCT TRIM(e.fieldOfStudy) FROM EspritProfile e WHERE e.fieldOfStudy IS NOT NULL AND TRIM(e.fieldOfStudy) <> '' ORDER BY TRIM(e.fieldOfStudy)")
    List<String> findDistinctFieldOfStudyValues();
}
