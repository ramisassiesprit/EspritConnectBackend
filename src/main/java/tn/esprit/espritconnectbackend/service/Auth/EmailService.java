package tn.esprit.espritconnectbackend.service.Auth;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
}
