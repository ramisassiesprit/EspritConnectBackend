package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.EmailSettings;

import java.util.Optional;

@Repository
public interface EmailSettingsRepository extends JpaRepository<EmailSettings, Long> {

    Optional<EmailSettings> findBySettingKey(String settingKey);
}
