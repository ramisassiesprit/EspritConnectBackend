package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.esprit.espritconnectbackend.dto.CommentDTO;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.Comment;
import tn.esprit.espritconnectbackend.entities.Post;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.repositories.CommentRepository;
import tn.esprit.espritconnectbackend.repositories.PostRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // ── Helper: get logged-in user ─────────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Helper: map Comment → CommentDTO ───────────────────────────────────
    private CommentDTO mapToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPost().getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());

        if (comment.getParent() != null) {
            dto.setParentId(comment.getParent().getId());
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setId(comment.getUser().getId());
        userDTO.setFirstName(comment.getUser().getFirstName());
        userDTO.setLastName(comment.getUser().getLastName());
        userDTO.setEmail(comment.getUser().getEmail());
        dto.setUser(userDTO);

        if (comment.getReplies() != null) {
            dto.setReplies(comment.getReplies()
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    @Override
    public CommentDTO addComment(UUID postId, String content, UUID parentId) {
        User currentUser = getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment.CommentBuilder builder = Comment.builder()
                .post(post)
                .user(currentUser)
                .content(content);

        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            builder.parent(parent);
        }

        // increment comments count
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return mapToDTO(commentRepository.save(builder.build()));
    }

    @Override
    public CommentDTO updateComment(UUID commentId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        User currentUser = getCurrentUser();
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only edit your own comments");
        }

        comment.setContent(content);
        return mapToDTO(commentRepository.save(comment));
    }

    @Override
    public void deleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        User currentUser = getCurrentUser();
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only delete your own comments");
        }

        // decrement comments count
        Post post = comment.getPost();
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        postRepository.save(post);

        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDTO> getCommentsByPost(UUID postId) {
        return commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}