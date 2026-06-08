package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.MessageDTO;
import tn.esprit.espritconnectbackend.service.MessageService;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload MessageDTO messageDTO) {
        MessageDTO savedMsg = messageService.sendMessage(messageDTO);
        
        // Send to the receiver's queue
        messagingTemplate.convertAndSendToUser(
                messageDTO.getReceiverId().toString(), "/queue/messages",
                savedMsg
        );
        
        // Also send back to the sender to confirm and sync (timestamp, ID)
        messagingTemplate.convertAndSendToUser(
                messageDTO.getSenderId().toString(), "/queue/messages",
                savedMsg
        );
    }

    @GetMapping("/history/{user1Id}/{user2Id}")
    public ResponseEntity<List<MessageDTO>> getChatHistory(
            @PathVariable UUID user1Id,
            @PathVariable UUID user2Id) {
        return ResponseEntity.ok(messageService.getChatHistory(user1Id, user2Id));
    }

    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<MessageDTO>> getConversations(@PathVariable UUID userId) {
        return ResponseEntity.ok(messageService.getConversations(userId));
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<MessageDTO>> getUnreadMessages(@PathVariable UUID userId) {
        return ResponseEntity.ok(messageService.getUnreadMessages(userId));
    }

    @PutMapping("/read/{messageId}")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID messageId) {
        messageService.markAsRead(messageId);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<MessageDTO> updateMessage(
            @PathVariable UUID id,
            @RequestBody MessageDTO dto,
            Principal principal) {
        return ResponseEntity.ok(messageService.updateMessage(id, dto, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID id,
            Principal principal) {
        messageService.deleteMessage(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
