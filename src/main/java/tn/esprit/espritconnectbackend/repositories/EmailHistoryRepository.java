package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.EmailHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailHistoryRepository extends JpaRepository<EmailHistory, UUID> {

    List<EmailHistory> findAllByOrderBySentAtDesc();

    List<EmailHistory> findByEmailTypeOrderBySentAtDesc(String emailType);

    long countByStatus(String status);

    long countBySentAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(e) FROM EmailHistory e")
    long countTotal();

    @Query("SELECT COUNT(DISTINCT e.recipientEmail) FROM EmailHistory e")
    long countDistinctRecipients();

    @Query("SELECT e.emailType, COUNT(e) FROM EmailHistory e GROUP BY e.emailType")
    List<Object[]> countByEmailType();
}
