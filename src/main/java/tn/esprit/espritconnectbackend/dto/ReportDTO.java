package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReportDTO {
    private UUID id;
    private UserDTO reporter;
    private String targetType;
    private UUID targetId;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
