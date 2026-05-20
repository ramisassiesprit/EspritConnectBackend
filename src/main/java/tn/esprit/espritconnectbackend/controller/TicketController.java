package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.TicketCommentDTO;
import tn.esprit.espritconnectbackend.dto.TicketPostDTO;
import tn.esprit.espritconnectbackend.service.TicketService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TicketController {

    private final TicketService ticketService;
    private final SimpMessagingTemplate messagingTemplate;

    // ── REST API Endpoints ───────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<TicketPostDTO>> getAllPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ticketService.getAllPosts(category, status, search));
    }

    @PostMapping
    public ResponseEntity<TicketPostDTO> createPost(@RequestBody TicketPostDTO request) {
        TicketPostDTO created = ticketService.createPost(request.getTitle(), request.getContent(), request.getCategory());
        // Broadcast the new post to all users via WebSocket
        messagingTemplate.convertAndSend("/topic/qa.newPost", created);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketPostDTO> getPostById(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getPostById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TicketPostDTO> togglePostStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        TicketPostDTO updated = ticketService.togglePostStatus(id, status);
        // Broadcast updated status to everyone viewing this post
        messagingTemplate.convertAndSend("/topic/qa.status." + id, updated);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<TicketPostDTO> upvotePost(@PathVariable UUID id) {
        TicketPostDTO updated = ticketService.upvotePost(id);
        // Broadcast the updated upvotes to everyone
        messagingTemplate.convertAndSend("/topic/qa.upvote." + id, updated);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketCommentDTO> addComment(
            @PathVariable UUID id,
            @RequestBody TicketCommentDTO request) {
        TicketCommentDTO created = ticketService.addComment(id, request.getContent());
        // Broadcast the new comment to everyone viewing this post
        messagingTemplate.convertAndSend("/topic/qa.comments." + id, created);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<TicketCommentDTO> upvoteComment(@PathVariable UUID commentId) {
        TicketCommentDTO updated = ticketService.upvoteComment(commentId);
        // Broadcast the updated comment (with new upvote state) to the post subscribers
        messagingTemplate.convertAndSend("/topic/qa.comments." + updated.getTicketPostId(), updated);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/comments/{commentId}/solution")
    public ResponseEntity<TicketCommentDTO> markCommentAsSolution(@PathVariable UUID commentId) {
        TicketCommentDTO updated = ticketService.markCommentAsSolution(commentId);
        // Fetch full post to broadcast the post's status change as well
        TicketPostDTO postUpdated = ticketService.getPostById(updated.getTicketPostId());
        
        messagingTemplate.convertAndSend("/topic/qa.status." + postUpdated.getId(), postUpdated);
        messagingTemplate.convertAndSend("/topic/qa.comments." + postUpdated.getId(), updated);
        return ResponseEntity.ok(updated);
    }

    // ── WebSocket Endpoint Message Handlers ──────────────────────────────────

    @MessageMapping("/qa.typing/{postId}")
    public void typingIndicator(
            @DestinationVariable String postId,
            @Payload String username) {
        // Broadcast who is typing to everyone subscribing to this topic
        messagingTemplate.convertAndSend("/topic/qa.typing." + postId, username);
    }
}
