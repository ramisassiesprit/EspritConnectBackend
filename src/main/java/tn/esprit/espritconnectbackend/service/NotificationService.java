package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.NotificationDTO;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    NotificationDTO createNotification(User user, String title, String body, NotificationType type, String targetType, UUID targetId);
    List<NotificationDTO> getMyNotifications();
    void markAsRead(UUID notificationId);
    void markAllAsRead();
    long getUnreadCount();
}
