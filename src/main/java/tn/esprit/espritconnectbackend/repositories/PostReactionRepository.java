package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.Post;
import tn.esprit.espritconnectbackend.entities.PostReaction;
import tn.esprit.espritconnectbackend.entities.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, UUID> {

    Optional<PostReaction> findByPostAndUser(Post post, User user);

    boolean existsByPostAndUser(Post post, User user);

    void deleteByPostAndUser(Post post, User user);
}