package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.MessageDTO;
import tn.esprit.espritconnectbackend.entities.Message;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.repositories.MessageRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    private User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    @Override
    @Transactional
    public MessageDTO sendMessage(MessageDTO messageDTO) {
        User sender;
        if (messageDTO.getSenderId() != null) {
            sender = userRepository.findById(messageDTO.getSenderId())
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
        } else {
            sender = getCurrentUserEntity();
        }
        User receiver = userRepository.findById(messageDTO.getReceiverId()).orElseThrow();

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(messageDTO.getContent())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);
        return mapToDTO(saved);
    }

    @Override
    public List<MessageDTO> getChatHistory(UUID user1Id, UUID user2Id) {
        User user1 = userRepository.findById(user1Id).orElseThrow();
        User user2 = userRepository.findById(user2Id).orElseThrow();
        return messageRepository.findChatHistory(user1, user2).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getConversations(UUID userId) {
        User user = getCurrentUserEntity();
        return messageRepository.findLatestMessagesByUser(user).stream()
                .filter(
                        u-> u.getReceiver().getRole() == UserRole.ETUDIANT ||  u.getReceiver().getRole() == UserRole.ALUMNI
                )
                .map(m -> {
                    MessageDTO dto = mapToDTO(m);
                    // Determine the other participant
                    User otherUser = m.getSender().getId().equals(userId) ? m.getReceiver() : m.getSender();
                    dto.setReceiverId(otherUser.getId());
                    dto.setSenderName(otherUser.getFirstName() + " " + otherUser.getLastName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(UUID messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    @Override
    public List<MessageDTO> getUnreadMessages(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return messageRepository.findByReceiverAndIsReadFalse(user).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public void deleteMessage(UUID messageId, String currentUserEmail) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        if (message.getSender().getEmail().equals(currentUserEmail)) {
            messageRepository.delete(message);
        } else {
            throw new RuntimeException("Action non autorisée");
        }
    }

    @Override
    @Transactional
    public MessageDTO updateMessage(UUID messageId, MessageDTO messageDTO, String currentUserEmail) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        if (message.getSender().getEmail().equals(currentUserEmail)) {
            message.setContent(messageDTO.getContent());
            // Pas besoin de save si @Transactional est actif, l'entité est managée
            return mapToDTO(message);
        } else {
            throw new RuntimeException("Action non autorisée");
        }
    }

    private MessageDTO mapToDTO(Message m) {
        return MessageDTO.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFirstName() + " " + m.getSender().getLastName())
                .receiverId(m.getReceiver().getId())
                .content(m.getContent())
                .isRead(m.getIsRead())
                .sentAt(m.getSentAt())
                .build();
    }
}
