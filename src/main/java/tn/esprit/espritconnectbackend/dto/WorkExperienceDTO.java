package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class WorkExperienceDTO {
    private UUID id;

    @Size(max = 200, message = "Le nom de l'entreprise ne doit pas depasser 200 caracteres")
    private String company;

    @Size(max = 150, message = "L'intitule du poste ne doit pas depasser 150 caracteres")
    private String jobTitle;

    @Size(max = 150, message = "L'industrie ne doit pas depasser 150 caracteres")
    private String industry;

    @Size(max = 150, message = "La fonction ne doit pas depasser 150 caracteres")
    private String jobFunction;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;

    @Size(max = 10000, message = "La description est trop longue")
    private String description;
}
