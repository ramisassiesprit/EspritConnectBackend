package tn.esprit.espritconnectbackend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MentoringSessionDTO {
    private UUID id;
    private UUID requestId;
    private LocalDate sessionDate;
    private String objectives;
    private String notes;
    private Integer rating;
    private String feedback;
    private LocalDateTime createdAt;
}
