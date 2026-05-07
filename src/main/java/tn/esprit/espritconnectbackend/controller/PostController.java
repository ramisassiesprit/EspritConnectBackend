package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.PostDTO;
import tn.esprit.espritconnectbackend.service.PostService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // Create a new post
    @PostMapping
    public ResponseEntity<PostDTO> createPost(
            @RequestParam String content,
            @RequestParam(required = false) String mediaUrl,
            @RequestParam(required = false) String postType) {
        return ResponseEntity.ok(postService.createPost(content, mediaUrl, postType));
    }

    // Get all posts (feed)
    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // Get a single post by ID
    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable UUID postId) {
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    // Get posts by a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(postService.getPostsByUser(userId));
    }

    // Update a post
    @PutMapping("/{postId}")
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable UUID postId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String mediaUrl) {
        return ResponseEntity.ok(postService.updatePost(postId, content, mediaUrl));
    }

    // Delete a post
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}