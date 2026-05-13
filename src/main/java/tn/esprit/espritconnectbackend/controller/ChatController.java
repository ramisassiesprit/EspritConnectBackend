package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.MessageDTO;
import tn.esprit.espritconnectbackend.service.MessageService;

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
        // Send to the specific receiver's queue
        messagingTemplate.convertAndSendToUser(
                messageDTO.getReceiverId().toString(), "/queue/messages",
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
}
