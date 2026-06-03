package tn.esprit.espritconnectbackend.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {
    private List<JobRecommendationDto> jobs;
    private List<EventRecommendationDto> events;
    private List<GroupRecommendationDto> groups;
    private List<MentorRecommendationDto> mentors;
}
