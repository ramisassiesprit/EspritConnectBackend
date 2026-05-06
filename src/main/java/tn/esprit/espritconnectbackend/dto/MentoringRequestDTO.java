package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.MentoringStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MentoringRequestDTO {
    private UUID id;
    private UserDTO mentee;
    private UserDTO mentor;
    private String message;
    private MentoringStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime updatedAt;
}
