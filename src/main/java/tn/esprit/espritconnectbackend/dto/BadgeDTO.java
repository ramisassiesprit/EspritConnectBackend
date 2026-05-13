package tn.esprit.espritconnectbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeDTO {
    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private String type;
    private LocalDateTime earnedAt;
}
