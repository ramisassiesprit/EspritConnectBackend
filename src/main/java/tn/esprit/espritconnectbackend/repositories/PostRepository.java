package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.Post;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Post> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

    List<Post> findAllByOrderByCreatedAtDesc();
}