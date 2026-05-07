package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.PostReactionDTO;
import tn.esprit.espritconnectbackend.service.PostReactionService;

import java.util.UUID;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
public class PostReactionController {

    private final PostReactionService postReactionService;

    // React to a post (like, love, etc.)
    @PostMapping("/{postId}")
    public ResponseEntity<PostReactionDTO> reactToPost(
            @PathVariable UUID postId,
            @RequestParam String reactionType) {
        return ResponseEntity.ok(postReactionService.reactToPost(postId, reactionType));
    }

    // Get my reaction on a post
    @GetMapping("/{postId}")
    public ResponseEntity<PostReactionDTO> getMyReaction(@PathVariable UUID postId) {
        return ResponseEntity.ok(postReactionService.getMyReaction(postId));
    }

    // Remove my reaction from a post
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> removeReaction(@PathVariable UUID postId) {
        postReactionService.removeReaction(postId);
        return ResponseEntity.noContent().build();
    }
}