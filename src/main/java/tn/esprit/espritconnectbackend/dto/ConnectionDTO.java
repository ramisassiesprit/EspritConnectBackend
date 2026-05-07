package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.ConnectionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ConnectionDTO {
    private UUID id;
    private UUID requesterId;
    private UUID addresseeId;
    private ConnectionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
