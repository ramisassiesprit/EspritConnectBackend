package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final UserRepository userRepository;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userIdStr = (String) headerAccessor.getSessionAttributes().get("userId");
        if (userIdStr != null) {
            updateUserStatus(UUID.fromString(userIdStr), true);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userIdStr = (String) headerAccessor.getSessionAttributes().get("userId");
        if (userIdStr != null) {
            updateUserStatus(UUID.fromString(userIdStr), false);
        }
    }

    private void updateUserStatus(UUID userId, boolean isOnline) {
        Optional<User> userOpt = userRepository.findById(userId);
        userOpt.ifPresent(user -> {
            user.setIsOnline(isOnline);
            userRepository.save(user);
            log.info("User {} status updated to {}", user.getEmail(), isOnline ? "ONLINE" : "OFFLINE");
        });
    }
}
