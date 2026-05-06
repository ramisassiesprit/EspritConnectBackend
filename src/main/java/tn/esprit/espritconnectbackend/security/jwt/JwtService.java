package tn.esprit.espritconnectbackend.security.jwt;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;
import tn.esprit.espritconnectbackend.entities.User;

import java.util.Date;
import java.util.function.Function;

public interface JwtService {
    String extractUsername(String token);
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);
    String generateAccessToken(User userDetails);
    String generateRefreshToken(User userDetails);
    boolean isTokenValid(String token, UserDetails userDetails);
    Date extractExpiration(String token);
}
