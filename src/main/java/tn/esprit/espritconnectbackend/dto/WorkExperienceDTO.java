package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class WorkExperienceDTO {
    private UUID id;
    private String company;
    private String jobTitle;
    private String industry;
    private String jobFunction;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private String description;
}
