package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.PostDTO;
import tn.esprit.espritconnectbackend.dto.PostFileDTO;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.Group;
import tn.esprit.espritconnectbackend.entities.Post;
import tn.esprit.espritconnectbackend.entities.PostFile;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.PostType;
import tn.esprit.espritconnectbackend.repositories.GroupRepository;
import tn.esprit.espritconnectbackend.repositories.PostRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final FileStorageService fileStorageService;

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
        if (post.getGroup() != null) {
            dto.setGroupId(post.getGroup().getId());
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setId(post.getUser().getId());
        userDTO.setFirstName(post.getUser().getFirstName());
        userDTO.setLastName(post.getUser().getLastName());
        userDTO.setEmail(post.getUser().getEmail());
        dto.setUser(userDTO);

        List<String> images = new java.util.ArrayList<>();
        List<PostFileDTO> files = new java.util.ArrayList<>();

        if (post.getFiles() != null) {
            for (PostFile file : post.getFiles()) {
                String ext = "";
                if (file.getName().contains(".")) {
                    ext = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
                }
                if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || 
                    ext.equals("gif") || ext.equals("bmp") || ext.equals("webp")) {
                    images.add(file.getUrl());
                } else {
                    PostFileDTO fileDTO = new PostFileDTO();
                    fileDTO.setId(file.getId());
                    fileDTO.setName(file.getName());
                    fileDTO.setSize(file.getSize());
                    fileDTO.setType(file.getType());
                    fileDTO.setUrl(file.getUrl());
                    files.add(fileDTO);
                }
            }
        }
        dto.setImages(images);
        dto.setFiles(files);

        dto.setLiked(false);
        try {
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                String email = auth.getName();
                if (post.getReactions() != null) {
                    boolean hasLiked = post.getReactions().stream()
                            .anyMatch(r -> r.getUser().getEmail().equals(email));
                    dto.setLiked(hasLiked);
                }
            }
        } catch (Exception e) {
            // ignore
        }

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
    public PostDTO createPostWithFiles(String content, UUID groupId, String postType, String mediaUrl, List<MultipartFile> files) throws IOException {
        User currentUser = getCurrentUser();
        
        Group group = null;
        if (groupId != null) {
            group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
        }

        PostType pType = PostType.TEXT;
        if (postType != null) {
            try {
                pType = PostType.valueOf(postType.toUpperCase());
            } catch (IllegalArgumentException e) {
                pType = PostType.TEXT;
            }
        }

        Post post = Post.builder()
                .user(currentUser)
                .group(group)
                .content(content)
                .mediaUrl(mediaUrl)
                .postType(pType)
                .build();

        List<PostFile> postFiles = new java.util.ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String savedPath = fileStorageService.savePostFile(file);
                    PostFile postFile = PostFile.builder()
                            .post(post)
                            .name(file.getOriginalFilename())
                            .size(file.getSize())
                            .type(file.getContentType())
                            .url(savedPath)
                            .build();
                    postFiles.add(postFile);
                }
            }
        }

        // If files exist, set post type dynamically if not specified or defaults to TEXT
        if (!postFiles.isEmpty() && pType == PostType.TEXT) {
            boolean hasImage = false;
            for (PostFile pf : postFiles) {
                String ext = "";
                if (pf.getName().contains(".")) {
                    ext = pf.getName().substring(pf.getName().lastIndexOf(".") + 1).toLowerCase();
                }
                if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || 
                    ext.equals("gif") || ext.equals("bmp") || ext.equals("webp")) {
                    hasImage = true;
                    break;
                }
            }
            post.setPostType(hasImage ? PostType.IMAGE : PostType.TEXT);
        }

        post.setFiles(postFiles);

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

        if (content != null)
            post.setContent(content);
        if (mediaUrl != null)
            post.setMediaUrl(mediaUrl);

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

    @Override
    public List<PostDTO> getPostsByGroup(UUID groupId) {
        return postRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}