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

    private tn.esprit.espritconnectbackend.entities.enums.UserRole role;
    private UserStatus status;
    private Integer numTel;
    private String avatarUrl;
    private String bannerUrl;
    private String bio;
    @Size(max = 100)
    private String code;

    @Size(max = 255)
    private String companyName;

    @Size(max = 100)
    private String jobTitle;

    @Size(max = 100)
    private String industry;

    @Size(max = 100)
    private String jobFunction;
    private String linkedinUrl;
    private String githubUrl;
    private String facebookUrl;
    private Boolean isMentor;
    private Boolean mentorAvailable;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isOnline;
    private java.util.List<BadgeDTO> badges;
    private EspritProfileDTO espritProfile;
    private java.util.List<WillingToHelpDTO> willingToHelps;
    private java.util.List<WorkExperienceDTO> workExperiences;
    private java.util.List<OtherEducationDTO> otherEducations;
    private java.util.List<SkillDTO> skills;
}
