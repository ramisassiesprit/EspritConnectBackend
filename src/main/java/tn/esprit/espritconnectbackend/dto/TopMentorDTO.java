package tn.esprit.espritconnectbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopMentorDTO {
    private String firstName;
    private String lastName;
    private long completedSessions;
    private long acceptedRequests;
    private long totalReceived;
    /** 0–100 */
    private double acceptanceRate;
    /** Average session rating 1–5, or null if no rated sessions */
    private Double avgRating;
}
