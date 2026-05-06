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

    @Value("${application.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Réinitialisation de votre mot de passe - Esprit Connect");
        message.setText("Pour réinitialiser votre mot de passe, veuillez cliquer sur le lien suivant :\n" +
                frontendUrl + "/reset-password?token=" + token + "\n\n" +
                "Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.");
        
        mailSender.send(message);
    }
}
