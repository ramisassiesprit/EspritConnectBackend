package tn.esprit.espritconnectbackend.service.Auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.espritconnectbackend.dto.AuthenticationRequest;
import tn.esprit.espritconnectbackend.dto.RegisterRequest;
import tn.esprit.espritconnectbackend.dto.ResetPasswordRequest;
import tn.esprit.espritconnectbackend.dto.AuthenticationResponse;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import tn.esprit.espritconnectbackend.security.jwt.JwtService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${application.security.jwt.expiration:900000}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration:604800000}")
    private long refreshExpiration;

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Tentative d'inscription avec un email déjà existant : {}", request.getEmail());
            throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
        }

        // Par défaut, tous les nouveaux comptes sont PENDING
        UserStatus initialStatus = UserStatus.PENDING;

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(initialStatus);

        userRepository.save(user);
        log.info("Nouvel utilisateur enregistré : {} ({}) avec le statut {}", user.getEmail(), user.getRole(), initialStatus);

        // Si le compte n'est pas actif, on ne retourne pas de token
        if (initialStatus != UserStatus.ACTIVE) {
            return new AuthenticationResponse();
        }

        String accessToken = jwtService.generateAccessToken(user);
        
        AuthenticationResponse authResponse = new AuthenticationResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setUserId(user.getId());
        authResponse.setRole(user.getRole());
        authResponse.setExpiresIn(jwtExpiration);
        return authResponse;
    }

    @Override
    public AuthenticationResponse login(AuthenticationRequest request, HttpServletRequest servletRequest, HttpServletResponse response) {
        log.info("Tentative de connexion pour l'utilisateur : {}", request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            log.error("Échec d'authentification pour {}: {}", request.getEmail(), e.getMessage());
            throw new org.springframework.security.authentication.BadCredentialsException("Email ou mot de passe incorrect");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Utilisateur non trouvé après authentification réussie (étrange) : {}", request.getEmail());
                    return new UsernameNotFoundException("Utilisateur non trouvé");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Connexion refusée pour {}: Compte non actif (statut: {})", user.getEmail(), user.getStatus());
            throw new IllegalArgumentException("Votre compte n'est pas actif. Statut : " + user.getStatus());
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        setRefreshTokenCookie(response, refreshToken, (int) (refreshExpiration / 1000));

        log.info("Connexion réussie pour l'utilisateur : {}", user.getEmail());
        
        AuthenticationResponse authResponse = new AuthenticationResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(refreshToken);
        authResponse.setUserId(user.getId());
        authResponse.setRole(user.getRole());
        authResponse.setExpiresIn(jwtExpiration);
        return authResponse;
    }

    @Override
    public AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token manquant");
        }

        String userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

            if (jwtService.isTokenValid(refreshToken, user)) {
                String accessToken = jwtService.generateAccessToken(user);
                
                // Optionnel : Rotation du refresh token
                String newRefreshToken = jwtService.generateRefreshToken(user);
                setRefreshTokenCookie(response, newRefreshToken, (int) (refreshExpiration / 1000));

                AuthenticationResponse authResponse = new AuthenticationResponse();
                authResponse.setAccessToken(accessToken);
                authResponse.setRefreshToken(newRefreshToken);
                authResponse.setUserId(user.getId());
                authResponse.setRole(user.getRole());
                authResponse.setExpiresIn(jwtExpiration);
                return authResponse;
            }
        }
        throw new IllegalArgumentException("Refresh token invalide");
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // Supprimer le cookie
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        // Dans un cas réel, on génèrerait un token spécifique stocké en base (PasswordResetToken)
        // Ici, on va utiliser le JwtService pour générer un token court de 15 minutes
        String resetToken = jwtService.generateAccessToken(user); // Hack rapide pour avoir un token avec expiration
        
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String email = jwtService.extractUsername(request.getToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        if (!jwtService.isTokenValid(request.getToken(), user)) {
            throw new IllegalArgumentException("Token de réinitialisation invalide ou expiré");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, int maxAge) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
