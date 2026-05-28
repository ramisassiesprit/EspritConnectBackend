package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.EventStatus;
import tn.esprit.espritconnectbackend.entities.enums.EventType;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventDTO {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String location;
    private String coverUrl;
    private Integer capacity;
    private Integer registeredCount;
    private String tags;
    private EventType eventType;
    private EventStatus status;
    private UUID creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double matchScore;
}
