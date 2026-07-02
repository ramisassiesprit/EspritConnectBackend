package tn.esprit.espritconnectbackend.service.Auth;

import tn.esprit.espritconnectbackend.entities.EmailHistory;

import java.util.List;
import java.util.Map;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
    void sendEventNotificationEmail(String to, String eventTitle, String eventDescription, String startAt, String location, String creatorName);
    void sendVideoChatEmail(String to, String topic, String message, String date, String meetLink, String senderName);

    EmailHistory sendCustomEmail(String to, String subject, String message, String imageUrl, String sentBy);
    void sendBirthdayEmail(String to, String firstName);
    List<EmailHistory> getEmailHistory();
    Map<String, Object> getEmailStats();
    boolean isBirthdayEmailEnabled();
    void setBirthdayEmailEnabled(boolean enabled);
    String getBirthdayEmailTemplate();
    void setBirthdayEmailTemplate(String template);
    List<Map<String, Object>> getUpcomingBirthdays();
    int sendBirthdayEmailsForToday();
}
