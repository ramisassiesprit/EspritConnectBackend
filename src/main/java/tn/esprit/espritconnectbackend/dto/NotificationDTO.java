package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationDTO {
    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private String targetType;
    private UUID targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
