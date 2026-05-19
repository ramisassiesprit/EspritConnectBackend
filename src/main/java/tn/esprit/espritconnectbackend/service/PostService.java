package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.PostDTO;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface PostService {

    PostDTO createPost(String content, String mediaUrl, String postType);

    PostDTO createPostWithFiles(String content, UUID groupId, String postType, String mediaUrl,
            List<MultipartFile> files) throws IOException;

    PostDTO updatePost(UUID postId, String content, String mediaUrl);

    void deletePost(UUID postId);

    PostDTO getPostById(UUID postId);

    List<PostDTO> getAllPosts();

    List<PostDTO> getPostsByUser(UUID userId);

    List<PostDTO> getPostsByGroup(UUID groupId);
}