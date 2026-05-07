package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.CommentDTO;
import tn.esprit.espritconnectbackend.service.CommentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // Add a comment to a post
    @PostMapping
    public ResponseEntity<CommentDTO> addComment(
            @RequestParam UUID postId,
            @RequestParam String content,
            @RequestParam(required = false) UUID parentId) {
        return ResponseEntity.ok(commentService.addComment(postId, content, parentId));
    }

    // Get all comments for a post
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }

    // Update a comment
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable UUID commentId,
            @RequestParam String content) {
        return ResponseEntity.ok(commentService.updateComment(commentId, content));
    }

    // Delete a comment
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}