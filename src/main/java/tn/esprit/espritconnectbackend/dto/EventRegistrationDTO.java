package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.RegistrationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventRegistrationDTO {
    private UUID id;
    private UUID eventId;
    private UUID userId;
    private String userFullName;
    private RegistrationStatus status;
    private LocalDateTime registeredAt;
}
