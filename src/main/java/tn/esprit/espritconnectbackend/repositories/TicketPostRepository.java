package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.TicketPost;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketPostRepository extends JpaRepository<TicketPost, UUID> {
    List<TicketPost> findAllByOrderByCreatedAtDesc();
    List<TicketPost> findByCategoryOrderByCreatedAtDesc(String category);
    List<TicketPost> findByStatusOrderByCreatedAtDesc(String status);
    List<TicketPost> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);
    List<TicketPost> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(String title, String content);
}
