package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.VideoChatDTO;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.Notification;
import tn.esprit.espritconnectbackend.entities.enums.NotificationType;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import tn.esprit.espritconnectbackend.service.Auth.EmailService;
import tn.esprit.espritconnectbackend.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

@RestController
@RequestMapping("/api/video-chat")
@RequiredArgsConstructor
public class VideoChatController {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleVideoChat(@RequestBody VideoChatDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User sender = (User) authentication.getPrincipal();

        User receiver = userRepository.findById(dto.getReceiverId()).orElseThrow(() -> new RuntimeException("User not found"));

        // Generate a random meet link if none provided.
        // Google Meet strictly verifies meeting codes against their servers, so randomly generated ones will show a "Check your meeting code" error.
        // For development/mock purposes, we use Jitsi Meet which automatically spins up a real room based on the URL.
        String meetLink = dto.getMeetLink();
        if (meetLink == null || meetLink.isEmpty()) {
            meetLink = "https://meet.jit.si/EspritConnect-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // Send Email
        emailService.sendVideoChatEmail(
                receiver.getEmail(),
                dto.getTopic(),
                dto.getMessage(),
                dto.getDate(),
                meetLink,
                sender.getFirstName() + " " + sender.getLastName()
        );

        // Send Notification to Receiver
        notificationService.createNotification(
                receiver,
                "Video Chat Scheduled",
                sender.getFirstName() + " " + sender.getLastName() + " has scheduled a video chat with you about: " + dto.getTopic() + " at " + dto.getDate() + ". Link: " + meetLink,
                NotificationType.SYSTEM,
                "VIDEO_CHAT_URL",
                null
        );

        // Send Notification to Sender
        notificationService.createNotification(
                sender,
                "Video Chat Scheduled",
                "You have scheduled a video chat with " + receiver.getFirstName() + " about: " + dto.getTopic() + " at " + dto.getDate() + ". Link: " + meetLink,
                NotificationType.SYSTEM,
                "VIDEO_CHAT_URL",
                null
        );

        return ResponseEntity.ok(java.util.Map.of("message", "Video chat scheduled successfully", "meetLink", meetLink));
    }
}