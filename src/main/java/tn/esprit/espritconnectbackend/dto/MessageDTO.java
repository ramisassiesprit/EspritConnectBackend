package tn.esprit.espritconnectbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MessageDTO {
    private UUID id;
    private UserDTO sender;
    private UUID receiverId;
    private String content;
    private Boolean isRead;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
