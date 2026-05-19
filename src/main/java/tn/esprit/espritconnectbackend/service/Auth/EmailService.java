package tn.esprit.espritconnectbackend.service.Auth;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
    void sendEventNotificationEmail(String to, String eventTitle, String eventDescription, String startAt, String location, String creatorName);
}
