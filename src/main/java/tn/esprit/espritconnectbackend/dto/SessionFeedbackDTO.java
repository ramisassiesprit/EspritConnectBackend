package tn.esprit.espritconnectbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionFeedbackDTO {
    private String mentorName;
    private String menteeName;
    /** 1–5, may be null */
    private Integer rating;
    private String feedback;
    private LocalDate sessionDate;
}
