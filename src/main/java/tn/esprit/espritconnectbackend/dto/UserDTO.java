package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String firstName;
    private String lastName;
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
