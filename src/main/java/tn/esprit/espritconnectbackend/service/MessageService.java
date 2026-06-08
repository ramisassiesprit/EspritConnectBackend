package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.MessageDTO;
import java.util.List;
import java.util.UUID;

public interface MessageService {
    MessageDTO sendMessage(MessageDTO messageDTO);

    List<MessageDTO> getChatHistory(UUID user1Id, UUID user2Id);

    List<MessageDTO> getConversations(UUID userId);

    void markAsRead(UUID messageId);

    List<MessageDTO> getUnreadMessages(UUID userId);

    void deleteMessage(UUID messageId, String currentUserEmail);

    MessageDTO updateMessage(UUID messageId, MessageDTO messageDTO, String currentUserEmail);
}
