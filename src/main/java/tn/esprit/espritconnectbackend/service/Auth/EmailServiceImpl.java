package tn.esprit.espritconnectbackend.service.Auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@espritconnect.com}")
    private String fromEmail;

    @Value("${application.frontend.url:http://localhost:4200/etudiant}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Réinitialisation de votre mot de passe - Esprit Connect");

            String htmlContent = "<div style=\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px; background-color: #ffffff;\">" +
                    "  <div style=\"text-align: center; border-bottom: 2px solid #C00000; padding-bottom: 20px; margin-bottom: 20px;\">" +
                    "    <h1 style=\"color: #C00000; margin: 0; font-size: 26px; font-weight: bold;\">Esprit Connect</h1>" +
                    "  </div>" +
                    "  <div style=\"padding: 10px 20px; color: #333333; line-height: 1.6;\">" +
                    "    <h2 style=\"font-size: 20px; color: #111111; margin-top: 0;\">Bonjour,</h2>" +
                    "    <p style=\"font-size: 16px;\">Vous avez demandé la réinitialisation du mot de passe de votre compte Esprit Connect. Pour procéder au changement, veuillez cliquer sur le bouton ci-dessous :</p>" +
                    "    <div style=\"text-align: center; margin: 30px 0;\">" +
                    "      <a href=\"" + resetUrl + "\" style=\"background-color: #C00000; color: #ffffff; text-decoration: none; padding: 14px 28px; font-size: 16px; font-weight: bold; border-radius: 5px; display: inline-block; box-shadow: 0 4px 6px rgba(192, 0, 0, 0.2); transition: background-color 0.3s ease;\">Réinitialiser mon mot de passe</a>" +
                    "    </div>" +
                    "    <p style=\"font-size: 14px; color: #666666;\">Ce lien est valide pour une durée de 15 minutes. Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail en toute sécurité.</p>" +
                    "  </div>" +
                    "  <div style=\"border-top: 1px solid #eeeeee; padding-top: 20px; margin-top: 20px; text-align: center; font-size: 12px; color: #999999;\">" +
                    "    <p>© " + java.time.Year.now().getValue() + " Esprit Connect. Tous droits réservés.</p>" +
                    "  </div>" +
                    "</div>";

            helper.setText(htmlContent, true); // true indicates HTML
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("=========================================================================");
            System.err.println("⚠️ IMPOSSIBLE D'ENVOYER L'EMAIL (Aucun serveur SMTP actif)");
            System.err.println("🔗 LIEN DE RÉINITIALISATION DE SECOURS GÉNÉRÉ POUR LE TEST :");
            System.err.println("   " + resetUrl);
            System.err.println("=========================================================================");
        }
    }

    @Override
    public void sendEventNotificationEmail(String to, String eventTitle, String eventDescription, String startAt, String location, String creatorName) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Nouvel événement publié - Esprit Connect");

            String htmlContent = "<div style=\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px; background-color: #ffffff;\">" +
                    "  <div style=\"text-align: center; border-bottom: 2px solid #C00000; padding-bottom: 20px; margin-bottom: 20px;\">" +
                    "    <h1 style=\"color: #C00000; margin: 0; font-size: 26px; font-weight: bold;\">Esprit Connect</h1>" +
                    "  </div>" +
                    "  <div style=\"padding: 10px 20px; color: #333333; line-height: 1.6;\">" +
                    "    <h2 style=\"font-size: 20px; color: #111111; margin-top: 0;\">Nouvel événement disponible !</h2>" +
                    "    <p style=\"font-size: 16px;\">Un nouvel événement vient d'être publié sur la plateforme. Rejoignez-nous pour y participer !</p>" +
                    "    <div style=\"background-color: #f9f9f9; padding: 15px; border-left: 4px solid #C00000; margin: 20px 0; border-radius: 4px;\">" +
                    "      <h3 style=\"margin-top: 0; color: #C00000;\">" + eventTitle + "</h3>" +
                    "      <p style=\"margin: 5px 0;\"><strong>Organisé par :</strong> " + creatorName + "</p>" +
                    "      <p style=\"margin: 5px 0;\"><strong>Date :</strong> " + startAt + "</p>" +
                    "      <p style=\"margin: 5px 0;\"><strong>Lieu :</strong> " + location + "</p>" +
                    "      <p style=\"margin: 10px 0 0 0;\">" + (eventDescription != null ? eventDescription : "") + "</p>" +
                    "    </div>" +
                    "    <div style=\"text-align: center; margin: 30px 0;\">" +
                    "      <a href=\"" + frontendUrl + "/events\" style=\"background-color: #C00000; color: #ffffff; text-decoration: none; padding: 14px 28px; font-size: 16px; font-weight: bold; border-radius: 5px; display: inline-block; box-shadow: 0 4px 6px rgba(192, 0, 0, 0.2); transition: background-color 0.3s ease;\">Voir l'événement</a>" +
                    "    </div>" +
                    "  </div>" +
                    "  <div style=\"border-top: 1px solid #eeeeee; padding-top: 20px; margin-top: 20px; text-align: center; font-size: 12px; color: #999999;\">" +
                    "    <p>© " + java.time.Year.now().getValue() + " Esprit Connect. Tous droits réservés.</p>" +
                    "  </div>" +
                    "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("=========================================================================");
            System.err.println("⚠️ IMPOSSIBLE D'ENVOYER L'EMAIL (Aucun serveur SMTP actif)");
            System.err.println("🔗 DÉTAILS DE L'ÉVÉNEMENT (De secours) :");
            System.err.println("   Titre: " + eventTitle);
            System.err.println("   Créateur: " + creatorName);
            System.err.println("=========================================================================");
        }
    }

    @Override
    public void sendVideoChatEmail(String to, String topic, String message, String date, String meetLink, String senderName) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Invitation to a Video Chat: " + topic);

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; padding: 20px; border: 1px solid #ddd; border-radius: 8px;\">" +
                    "<h2 style=\"color: #C00000;\">Video Chat Invitation</h2>" +
                    "<p>Hello,</p>" +
                    "<p><b>" + senderName + "</b> has invited you to a video chat.</p>" +
                    "<p><b>Topic:</b> " + topic + "</p>" +
                    "<p><b>Date:</b> " + date + "</p>" +
                    "<p><b>Message:</b> " + message + "</p>" +
                    "<br/>" +
                    "<div style=\"text-align: center;\">" +
                    "<a href=\"" + meetLink + "\" style=\"background-color: #C00000; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;\">Join Google Meet</a>" +
                    "</div>" +
                    "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("⚠️ Could not send video chat email: " + e.getMessage());
        }
    }
}
