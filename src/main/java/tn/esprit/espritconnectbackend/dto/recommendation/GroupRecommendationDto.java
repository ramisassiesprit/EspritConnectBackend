package tn.esprit.espritconnectbackend.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRecommendationDto {
    private UUID id;
    private String groupName;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String privacy;
    private Integer membersCount;
}
