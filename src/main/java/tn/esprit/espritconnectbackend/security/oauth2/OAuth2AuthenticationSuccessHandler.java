package tn.esprit.espritconnectbackend.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import tn.esprit.espritconnectbackend.security.jwt.JwtService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.security.jwt.refresh-token.expiration:604800000}")
    private long refreshExpiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.info("OAuth2 login success: attributes={}", oAuth2User.getAttributes());

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            // LinkedIn using OAuth2 OpenID Connect uses 'email' field or similar, Google uses 'email'
            email = oAuth2User.getAttribute("email");
        }

        if (email == null) {
            log.error("Email not found in OAuth2 attributes");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email non trouvé via le fournisseur de connexion");
            return;
        }

        String finalEmail = email;
        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            // Extract names
            String firstName = oAuth2User.getAttribute("given_name");
            if (firstName == null) {
                firstName = oAuth2User.getAttribute("localizedFirstName");
            }
            if (firstName == null) {
                firstName = oAuth2User.getAttribute("name");
            }
            if (firstName == null) {
                firstName = "Social";
            }

            String lastName = oAuth2User.getAttribute("family_name");
            if (lastName == null) {
                lastName = oAuth2User.getAttribute("localizedLastName");
            }
            if (lastName == null) {
                lastName = "User";
            }

            String picture = oAuth2User.getAttribute("picture");
            if (picture == null) {
                picture = oAuth2User.getAttribute("profilePicture");
            }

            User newUser = User.builder()
                    .email(finalEmail)
                    .firstName(firstName)
                    .lastName(lastName)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                    .role(UserRole.ETUDIANT) // Default to ETUDIANT
                    .status(UserStatus.ACTIVE)
                    .avatarUrl(picture)
                    .build();
            log.info("Creating new user via OAuth2: {}", finalEmail);
            return userRepository.save(newUser);
        });

        // Update login stats
        user.setLastLoginAt(LocalDateTime.now());
        user.setIsOnline(true);
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Set refresh token cookie
        setRefreshTokenCookie(response, refreshToken, (int) (refreshExpiration / 1000));

        // Safely encode and filter avatarUrl to prevent HeadersTooLargeException if it contains a base64 data URI
        String safeAvatarUrl = "";
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().startsWith("data:") && user.getAvatarUrl().length() < 2048) {
            safeAvatarUrl = URLEncoder.encode(user.getAvatarUrl(), StandardCharsets.UTF_8);
        }

        // Redirect URL to Angular
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:4200/oauth2-redirect")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("userId", user.getId().toString())
                .queryParam("role", user.getRole().name())
                .queryParam("firstName", URLEncoder.encode(user.getFirstName(), StandardCharsets.UTF_8))
                .queryParam("lastName", URLEncoder.encode(user.getLastName(), StandardCharsets.UTF_8))
                .queryParam("email", user.getEmail())
                .queryParam("avatarUrl", safeAvatarUrl)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, int maxAge) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
