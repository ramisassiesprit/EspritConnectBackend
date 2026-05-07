package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.esprit.espritconnectbackend.dto.PostDTO;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.Post;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.PostType;
import tn.esprit.espritconnectbackend.repositories.PostRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // ── Helper: get logged-in user ─────────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Helper: map Post → PostDTO ─────────────────────────────────────────
    private PostDTO mapToDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setContent(post.getContent());
        dto.setMediaUrl(post.getMediaUrl());
        dto.setPostType(post.getPostType());
        dto.setIsPinned(post.getIsPinned());
        dto.setLikesCount(post.getLikesCount());
        dto.setCommentsCount(post.getCommentsCount());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        UserDTO userDTO = new UserDTO();
        userDTO.setId(post.getUser().getId());
        userDTO.setFirstName(post.getUser().getFirstName());
        userDTO.setLastName(post.getUser().getLastName());
        userDTO.setEmail(post.getUser().getEmail());
        dto.setUser(userDTO);

        return dto;
    }

    @Override
    public PostDTO createPost(String content, String mediaUrl, String postType) {
        User currentUser = getCurrentUser();

        Post post = Post.builder()
                .user(currentUser)
                .content(content)
                .mediaUrl(mediaUrl)
                .postType(postType != null ? PostType.valueOf(postType) : PostType.TEXT)
                .build();

        return mapToDTO(postRepository.save(post));
    }

    @Override
    public PostDTO updatePost(UUID postId, String content, String mediaUrl) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User currentUser = getCurrentUser();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only edit your own posts");
        }

        if (content != null) post.setContent(content);
        if (mediaUrl != null) post.setMediaUrl(mediaUrl);

        return mapToDTO(postRepository.save(post));
    }

    @Override
    public void deletePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User currentUser = getCurrentUser();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only delete your own posts");
        }

        postRepository.delete(post);
    }

    @Override
    public PostDTO getPostById(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return mapToDTO(post);
    }

    @Override
    public List<PostDTO> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PostDTO> getPostsByUser(UUID userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}