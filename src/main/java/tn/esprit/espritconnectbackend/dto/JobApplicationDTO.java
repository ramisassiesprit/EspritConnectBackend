package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class JobApplicationDTO {
    private UUID id;

    @NotNull(message = "L'offre est obligatoire")
    private UUID jobOfferId;

    private UUID applicantId;
    private String cvUrl;
    private String coverLetterUrl;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantEmail;
    private Integer applicantNumTel;
    private String applicantAvatarUrl;
    private String applicantJobTitle;
    private String applicantIndustry;
    private String applicantJobFunction;
    private String applicantLinkedinUrl;
    private String applicantGithubUrl;
    private String applicantFacebookUrl;
    private String applicantBio;
    private Integer applicantGraduationYear;
    private String applicantProgram;
    private String applicantDegree;
    private String applicantFieldOfStudy;
    private String applicantInstitution;
    private ApplicationStatus status;
    private String aiSummary;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
