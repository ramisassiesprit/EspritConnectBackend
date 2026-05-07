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
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
