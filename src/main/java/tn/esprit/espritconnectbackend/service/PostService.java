package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.PostDTO;
import java.util.List;
import java.util.UUID;

public interface PostService {

    PostDTO createPost(String content, String mediaUrl, String postType);

    PostDTO updatePost(UUID postId, String content, String mediaUrl);

    void deletePost(UUID postId);

    PostDTO getPostById(UUID postId);

    List<PostDTO> getAllPosts();

    List<PostDTO> getPostsByUser(UUID userId);
}