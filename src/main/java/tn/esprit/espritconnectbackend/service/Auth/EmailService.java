package tn.esprit.espritconnectbackend.service.Auth;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
    void sendEventNotificationEmail(String to, String eventTitle, String eventDescription, String startAt, String location, String creatorName);
    void sendVideoChatEmail(String to, String topic, String message, String date, String meetLink, String senderName);
    void sendAdminCustomEmail(java.util.List<String> toEmails, String subject, String message);
}
