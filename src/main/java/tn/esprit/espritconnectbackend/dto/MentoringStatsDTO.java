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
public class MentoringStatsDTO {
    private long totalUsers;
    private long totalMentors;
    private long usersOfferingHelp;
    private long usersSeekingHelp;
    private long usersOfferingMentoring;
    private long usersSeekingMentoring;
    private double offerHelpPercentage;
    private double seekHelpPercentage;
    private double offerMentoringPercentage;
    private double seekMentoringPercentage;

    private Map<String, Long> offerHelpByOption;
    private Map<String, Long> seekHelpByOption;
    private Map<String, Long> offerMentoringByOption;
    private Map<String, Long> seekMentoringByOption;

    private long totalRequests;
    private long pendingRequests;
    private long acceptedRequests;
    private long rejectedRequests;
    private long completedRequests;
    private long cancelledRequests;

    private Map<String, Long> requestsByStatus;
    private Map<String, Long> requestsByMonth;
    private Map<String, Long> requestsByFieldOfStudy;

    private long totalSessions;
    private Double averageSessionRating;

    // ── New analytics (computed in-memory, no extra queries) ──────────────────

    private List<TopMentorDTO> topMentors;
    /** rating 1–5 → number of sessions */
    private Map<String, Long> ratingDistribution;
    private List<SessionFeedbackDTO> recentFeedback;
    private Map<String, Long> requestsByGraduationYear;
    private Map<String, Long> requestsByIndustry;
    /** option label → [supply (offer), demand (seek)] */
    private Map<String, long[]> supplyVsDemandByOption;
}
