package tn.esprit.espritconnectbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAdminStatsDTO {
    private long totalEvents;
    private long totalParticipants;
    private long totalEligibleUsers;
    private double participationRate;
    private double attendanceRate;
    private double averageFeedbackScore;
    private long totalWinners;
    
    private List<EventDTO> topPopularEvents;
    private Map<String, Long> eventsByStatus;
    private Map<String, Long> eventsByType;
    private Map<String, Long> registrationsByMonth;
}
