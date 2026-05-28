package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.Post;
import tn.esprit.espritconnectbackend.entities.enums.GroupPrivacy;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Post> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

    List<Post> findAllByOrderByCreatedAtDesc();
    // Dans l'interface PostRepository
    @Query("SELECT p FROM Post p LEFT JOIN p.group g " +
            "WHERE g IS NULL OR g.privacy = :privacy " +
            "ORDER BY p.createdAt DESC")
    List<Post> findAllVisiblePosts(@Param("privacy") GroupPrivacy privacy);
}