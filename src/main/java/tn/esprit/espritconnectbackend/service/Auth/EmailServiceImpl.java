package tn.esprit.espritconnectbackend.service.Auth;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.espritconnectbackend.entities.EmailHistory;
import tn.esprit.espritconnectbackend.entities.EmailSettings;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.repositories.EmailHistoryRepository;
import tn.esprit.espritconnectbackend.repositories.EmailSettingsRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailHistoryRepository emailHistoryRepository;
    private final EmailSettingsRepository emailSettingsRepository;
    private final UserRepository userRepository;

    @Value("${spring.mail.username:noreply@espritconnect.com}")
    private String fromEmail;

    @Value("${application.frontend.url:http://localhost:4200/etudiant}")
    private String frontendUrl;

    private static final String KEY_BIRTHDAY_ENABLED = "birthday_email_enabled";
    private static final String KEY_BIRTHDAY_TEMPLATE = "birthday_email_template";
    private static final String DEFAULT_BIRTHDAY_TEMPLATE =
            "<div style=\"font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px;background:#ffffff;\">"
            + "<div style=\"text-align:center;border-bottom:2px solid #C00000;padding-bottom:20px;margin-bottom:20px;\">"
            + "<h1 style=\"color:#C00000;margin:0;font-size:26px;font-weight:bold;\">Esprit Connect</h1></div>"
            + "<div style=\"padding:10px 20px;color:#333;line-height:1.6;\">"
            + "<h2 style=\"font-size:20px;color:#111;margin-top:0;\">Happy Birthday, {{firstName}}! 🎂</h2>"
            + "<p style=\"font-size:16px;\">The entire Esprit Connect team wishes you a fantastic birthday filled with joy and success!</p>"
            + "<p style=\"font-size:16px;\">May this year bring you new opportunities, achievements, and wonderful memories.</p>"
            + "<div style=\"text-align:center;margin:30px 0;\">"
            + "<a href=\"{{frontendUrl}}/feed\" style=\"background-color:#C00000;color:#ffffff;text-decoration:none;padding:14px 28px;font-size:16px;font-weight:bold;border-radius:5px;display:inline-block;\">Visit Esprit Connect</a></div>"
            + "<p style=\"font-size:14px;color:#666;\">Stay connected with your Esprit community!</p></div>"
            + "<div style=\"border-top:1px solid #eee;padding-top:20px;margin-top:20px;text-align:center;font-size:12px;color:#999;\">"
            + "<p>© " + java.time.Year.now().getValue() + " Esprit Connect. All rights reserved.</p></div></div>";

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Reset your password - Esprit Connect");
            String html = "<div style=\"font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px;background:#ffffff;\">"
                    + "<div style=\"text-align:center;border-bottom:2px solid #C00000;padding-bottom:20px;margin-bottom:20px;\"><h1 style=\"color:#C00000;margin:0;font-size:26px;font-weight:bold;\">Esprit Connect</h1></div>"
                    + "<div style=\"padding:10px 20px;color:#333;line-height:1.6;\"><h2 style=\"font-size:20px;color:#111;margin-top:0;\">Hello,</h2>"
                    + "<p style=\"font-size:16px;\">Click below to reset your password:</p>"
                    + "<div style=\"text-align:center;margin:30px 0;\"><a href=\"" + resetUrl + "\" style=\"background-color:#C00000;color:#ffffff;text-decoration:none;padding:14px 28px;font-size:16px;font-weight:bold;border-radius:5px;display:inline-block;\">Reset Password</a></div>"
                    + "<p style=\"font-size:14px;color:#666;\">This link expires in 15 minutes.</p></div>"
                    + "<div style=\"border-top:1px solid #eee;padding-top:20px;margin-top:20px;text-align:center;font-size:12px;color:#999;\">"
                    + "<p>© " + java.time.Year.now().getValue() + " Esprit Connect.</p></div></div>";
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Password reset email failed for " + to + ": " + e.getMessage());
        }
    }

    @Override
    public void sendEventNotificationEmail(String to, String eventTitle, String eventDescription, String startAt, String location, String creatorName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("New Event - Esprit Connect");
            String html = "<div style=\"font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px;background:#ffffff;\">"
                    + "<div style=\"text-align:center;border-bottom:2px solid #C00000;padding-bottom:20px;margin-bottom:20px;\"><h1 style=\"color:#C00000;margin:0;font-size:26px;font-weight:bold;\">Esprit Connect</h1></div>"
                    + "<div style=\"padding:10px 20px;color:#333;line-height:1.6;\"><h2 style=\"font-size:20px;color:#111;margin-top:0;\">New Event!</h2>"
                    + "<div style=\"background:#f9f9f9;padding:15px;border-left:4px solid #C00000;margin:20px 0;border-radius:4px;\">"
                    + "<h3 style=\"margin-top:0;color:#C00000;\">" + eventTitle + "</h3>"
                    + "<p><strong>By:</strong> " + creatorName + "</p><p><strong>Date:</strong> " + startAt + "</p><p><strong>Location:</strong> " + location + "</p>"
                    + "<p>" + (eventDescription != null ? eventDescription : "") + "</p></div></div></div>";
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Event notification email failed for " + to + ": " + e.getMessage());
        }
    }

    @Override
    public void sendVideoChatEmail(String to, String topic, String message, String date, String meetLink, String senderName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Video Chat Invitation: " + topic);
            String html = "<div style=\"font-family:Arial,sans-serif;max-width:600px;padding:20px;border:1px solid #ddd;border-radius:8px;\">"
                    + "<h2 style=\"color:#C00000;\">Video Chat Invitation</h2><p>Hello,</p>"
                    + "<p><b>" + senderName + "</b> invited you.</p><p><b>Topic:</b> " + topic + "</p><p><b>Date:</b> " + date + "</p><p><b>Message:</b> " + message + "</p>"
                    + "<div style=\"text-align:center;\"><a href=\"" + meetLink + "\" style=\"background-color:#C00000;color:white;padding:12px 24px;text-decoration:none;border-radius:5px;font-weight:bold;\">Join Meet</a></div></div>";
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Video chat email failed: " + e.getMessage());
        }
    }

    @Override
    public EmailHistory sendCustomEmail(String to, String subject, String message, String imageUrl, String sentBy) {
        EmailHistory history = EmailHistory.builder()
                .recipientEmail(to)
                .subject(subject)
                .messageBody(message)
                .emailType("CUSTOM")
                .sentAt(LocalDateTime.now())
                .sentBy(sentBy)
                .hasImage(imageUrl != null && !imageUrl.isEmpty())
                .build();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            StringBuilder html = new StringBuilder();
            html.append("<div style=\"font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px;background:#ffffff;\">");
            html.append("<div style=\"text-align:center;border-bottom:2px solid #C00000;padding-bottom:20px;margin-bottom:20px;\">");
            html.append("<h1 style=\"color:#C00000;margin:0;font-size:26px;font-weight:bold;\">Esprit Connect</h1></div>");
            html.append("<div style=\"padding:10px 20px;color:#333;line-height:1.6;\">");
            html.append(message.replace("\n", "<br/>"));
            if (imageUrl != null && !imageUrl.isEmpty()) {
                html.append("<div style=\"text-align:center;margin:20px 0;\">");
                html.append("<img src=\"").append(imageUrl).append("\" style=\"max-width:100%;border-radius:8px;\" alt=\"image\"/>");
                html.append("</div>");
            }
            html.append("</div>");
            html.append("<div style=\"border-top:1px solid #eee;padding-top:20px;margin-top:20px;text-align:center;font-size:12px;color:#999;\">");
            html.append("<p>© ").append(java.time.Year.now().getValue()).append(" Esprit Connect.</p></div></div>");

            helper.setText(html.toString(), true);
            mailSender.send(mimeMessage);
            history.setStatus("SENT");
        } catch (Exception e) {
            history.setStatus("FAILED");
            history.setErrorMessage(e.getMessage());
            System.err.println("Custom email failed to " + to + ": " + e.getMessage());
        }

        return emailHistoryRepository.save(history);
    }

    @Override
    public void sendAdminCustomEmail(List<String> emails, String subject, String message) {
        for (String email : emails) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(email);
                helper.setSubject(subject);

                StringBuilder html = new StringBuilder();
                html.append("<div style=\"font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px;background:#ffffff;\">");
                html.append("<div style=\"text-align:center;border-bottom:2px solid #C00000;padding-bottom:20px;margin-bottom:20px;\">");
                html.append("<h1 style=\"color:#C00000;margin:0;font-size:26px;font-weight:bold;\">Esprit Connect</h1></div>");
                html.append("<div style=\"padding:10px 20px;color:#333;line-height:1.6;\">");
                html.append(message.replace("\n", "<br/>"));
                html.append("</div>");
                html.append("<div style=\"border-top:1px solid #eee;padding-top:20px;margin-top:20px;text-align:center;font-size:12px;color:#999;\">");
                html.append("<p>© ").append(java.time.Year.now().getValue()).append(" Esprit Connect.</p></div></div>");

                helper.setText(html.toString(), true);
                mailSender.send(mimeMessage);

                emailHistoryRepository.save(EmailHistory.builder()
                        .recipientEmail(email)
                        .subject(subject)
                        .messageBody(message)
                        .emailType("ADMIN")
                        .sentAt(LocalDateTime.now())
                        .sentBy("ADMIN")
                        .status("SENT")
                        .build());
            } catch (Exception e) {
                System.err.println("Admin custom email failed to " + email + ": " + e.getMessage());
                emailHistoryRepository.save(EmailHistory.builder()
                        .recipientEmail(email)
                        .subject(subject)
                        .messageBody(message)
                        .emailType("ADMIN")
                        .sentAt(LocalDateTime.now())
                        .sentBy("ADMIN")
                        .status("FAILED")
                        .errorMessage(e.getMessage())
                        .build());
            }
        }
    }

    @Override
    public void sendBirthdayEmail(String to, String firstName) {
        String template = getBirthdayEmailTemplate();
        String body = template.replace("{{firstName}}", firstName);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Happy Birthday from Esprit Connect! 🎂");
            helper.setText(body, true);
            mailSender.send(mimeMessage);

            emailHistoryRepository.save(EmailHistory.builder()
                    .recipientEmail(to)
                    .recipientName(firstName)
                    .subject("Happy Birthday from Esprit Connect! 🎂")
                    .messageBody(body)
                    .emailType("BIRTHDAY")
                    .sentAt(LocalDateTime.now())
                    .status("SENT")
                    .build());
        } catch (Exception e) {
            System.err.println("Birthday email failed to " + to + ": " + e.getMessage());
            emailHistoryRepository.save(EmailHistory.builder()
                    .recipientEmail(to)
                    .recipientName(firstName)
                    .subject("Happy Birthday from Esprit Connect! 🎂")
                    .emailType("BIRTHDAY")
                    .sentAt(LocalDateTime.now())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build());
        }
    }

    @Override
    public List<EmailHistory> getEmailHistory() {
        return emailHistoryRepository.findAllByOrderBySentAtDesc();
    }

    @Override
    public Map<String, Object> getEmailStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSent", emailHistoryRepository.countTotal());
        stats.put("successful", emailHistoryRepository.countByStatus("SENT"));
        stats.put("failed", emailHistoryRepository.countByStatus("FAILED"));
        stats.put("uniqueRecipients", emailHistoryRepository.countDistinctRecipients());
        stats.put("sentToday", emailHistoryRepository.countBySentAtBetween(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)));
        stats.put("sentThisMonth", emailHistoryRepository.countBySentAtBetween(
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)));

        List<Object[]> typeCounts = emailHistoryRepository.countByEmailType();
        Map<String, Long> breakdown = new HashMap<>();
        for (Object[] row : typeCounts) {
            breakdown.put((String) row[0], (Long) row[1]);
        }
        stats.put("byType", breakdown);

        return stats;
    }

    @Override
    public boolean isBirthdayEmailEnabled() {
        return emailSettingsRepository.findBySettingKey(KEY_BIRTHDAY_ENABLED)
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue()))
                .orElse(false);
    }

    @Override
    public void setBirthdayEmailEnabled(boolean enabled) {
        EmailSettings setting = emailSettingsRepository.findBySettingKey(KEY_BIRTHDAY_ENABLED)
                .orElse(EmailSettings.builder()
                        .settingKey(KEY_BIRTHDAY_ENABLED)
                        .description("Enable automatic birthday emails")
                        .build());
        setting.setSettingValue(String.valueOf(enabled));
        emailSettingsRepository.save(setting);
    }

    @Override
    public String getBirthdayEmailTemplate() {
        String template = emailSettingsRepository.findBySettingKey(KEY_BIRTHDAY_TEMPLATE)
                .map(EmailSettings::getSettingValue)
                .orElse(DEFAULT_BIRTHDAY_TEMPLATE);
        return template.replace("{{frontendUrl}}", frontendUrl);
    }

    @Override
    public void setBirthdayEmailTemplate(String template) {
        EmailSettings setting = emailSettingsRepository.findBySettingKey(KEY_BIRTHDAY_TEMPLATE)
                .orElse(EmailSettings.builder()
                        .settingKey(KEY_BIRTHDAY_TEMPLATE)
                        .description("HTML template for birthday emails (use {{firstName}} placeholder)")
                        .build());
        setting.setSettingValue(template);
        emailSettingsRepository.save(setting);
    }

    @Override
    public List<Map<String, Object>> getUpcomingBirthdays() {
        List<User> allUsers = userRepository.findAll();
        MonthDay today = MonthDay.now();
        return allUsers.stream()
                .filter(u -> u.getDateOfBirth() != null)
                .map(u -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", u.getId());
                    item.put("firstName", u.getFirstName());
                    item.put("lastName", u.getLastName());
                    item.put("email", u.getEmail());
                    item.put("dateOfBirth", u.getDateOfBirth());
                    MonthDay bday = MonthDay.of(u.getDateOfBirth().getMonth(), u.getDateOfBirth().getDayOfMonth());
                    item.put("isToday", bday.equals(today));
                    int daysUntil = today.compareTo(bday) <= 0
                            ? bday.compareTo(today)
                            : 365 - Math.abs(bday.compareTo(today));
                    item.put("daysUntilBirthday", daysUntil);
                    return item;
                })
                .sorted(Comparator.comparingInt(m -> (int) m.get("daysUntilBirthday")))
                .limit(50)
                .collect(Collectors.toList());
    }

    @Override
    public int sendBirthdayEmailsForToday() {
        if (!isBirthdayEmailEnabled()) return 0;
        List<User> birthdayUsers = userRepository.findAll().stream()
                .filter(u -> u.getDateOfBirth() != null)
                .filter(u -> {
                    MonthDay today = MonthDay.now();
                    MonthDay bday = MonthDay.of(u.getDateOfBirth().getMonth(), u.getDateOfBirth().getDayOfMonth());
                    return bday.equals(today);
                })
                .collect(Collectors.toList());
        for (User user : birthdayUsers) {
            sendBirthdayEmail(user.getEmail(), user.getFirstName());
        }
        return birthdayUsers.size();
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void autoSendBirthdayEmails() {
        try {
            int sent = sendBirthdayEmailsForToday();
            System.out.println("Auto birthday email rund completed. Sent: " + sent);
        } catch (Exception e) {
            System.err.println("Auto birthday email rund failed: " + e.getMessage());
        }
    }
}
