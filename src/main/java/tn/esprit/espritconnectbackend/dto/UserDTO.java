package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    private UserRole role;
    private UserStatus status;
    private String avatarUrl;
    private String bannerUrl;
    private String bio;
    private String city;
    private String country;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private Boolean isMentor;
    private Boolean mentorAvailable;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
