package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.Message;
import tn.esprit.espritconnectbackend.entities.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.sentAt ASC")
    List<Message> findChatHistory(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT m FROM Message m WHERE m.sentAt IN (SELECT MAX(m2.sentAt) FROM Message m2 WHERE m2.sender = :user OR m2.receiver = :user GROUP BY (CASE WHEN m2.sender = :user THEN m2.receiver.id ELSE m2.sender.id END)) ORDER BY m.sentAt DESC")
    List<Message> findLatestMessagesByUser(@Param("user") User user);

    List<Message> findByReceiverAndIsReadFalse(User receiver);
}
