package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.PostReactionDTO;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.Post;
import tn.esprit.espritconnectbackend.entities.PostReaction;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.ReactionType;
import tn.esprit.espritconnectbackend.repositories.PostReactionRepository;
import tn.esprit.espritconnectbackend.repositories.PostRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostReactionServiceImpl implements PostReactionService {

    private final PostReactionRepository postReactionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // ── Helper: get logged-in user ─────────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Helper: map PostReaction → PostReactionDTO ─────────────────────────
    private PostReactionDTO mapToDTO(PostReaction reaction) {
        PostReactionDTO dto = new PostReactionDTO();
        dto.setId(reaction.getId());
        dto.setPostId(reaction.getPost().getId());
        dto.setReactionType(reaction.getReactionType());
        dto.setCreatedAt(reaction.getCreatedAt());

        UserDTO userDTO = new UserDTO();
        userDTO.setId(reaction.getUser().getId());
        userDTO.setFirstName(reaction.getUser().getFirstName());
        userDTO.setLastName(reaction.getUser().getLastName());
        userDTO.setEmail(reaction.getUser().getEmail());
        dto.setUser(userDTO);

        return dto;
    }

    @Override
    @Transactional
    public PostReactionDTO reactToPost(UUID postId, String reactionType) {
        User currentUser = getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // If reaction already exists → update it
        if (postReactionRepository.existsByPostAndUser(post, currentUser)) {
            PostReaction existing = postReactionRepository.findByPostAndUser(post, currentUser)
                    .orElseThrow();
            existing.setReactionType(ReactionType.valueOf(reactionType));
            return mapToDTO(postReactionRepository.save(existing));
        }

        // New reaction → increment likes count
        PostReaction reaction = PostReaction.builder()
                .post(post)
                .user(currentUser)
                .reactionType(ReactionType.valueOf(reactionType))
                .build();

        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);

        return mapToDTO(postReactionRepository.save(reaction));
    }

    @Override
    @Transactional
    public void removeReaction(UUID postId) {
        User currentUser = getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!postReactionRepository.existsByPostAndUser(post, currentUser)) {
            throw new RuntimeException("No reaction found");
        }

        postReactionRepository.deleteByPostAndUser(post, currentUser);

        // decrement likes count
        post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        postRepository.save(post);
    }

    @Override
    public PostReactionDTO getMyReaction(UUID postId) {
        User currentUser = getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        PostReaction reaction = postReactionRepository.findByPostAndUser(post, currentUser)
                .orElseThrow(() -> new RuntimeException("No reaction found"));

        return mapToDTO(reaction);
    }
}