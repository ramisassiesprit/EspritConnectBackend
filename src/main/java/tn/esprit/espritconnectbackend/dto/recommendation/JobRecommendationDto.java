package tn.esprit.espritconnectbackend.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRecommendationDto {
    private UUID id;
    private String title;
    private String company;
    private String location;
    private String description;
    private String contractType;
    private String experienceLevel;
    private LocalDate deadline;
    private String imageUrl;
    private String status;
}
