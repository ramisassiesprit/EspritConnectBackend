package tn.esprit.espritconnectbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private UUID userId;
    private UserRole role;
    private long expiresIn;
}
