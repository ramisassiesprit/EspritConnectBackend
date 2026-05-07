package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class OtherEducationDTO {
    private UUID id;

    @Size(max = 200, message = "Le nom de l'institution ne doit pas depasser 200 caracteres")
    private String institutionName;

    @Size(max = 100, message = "Le diplome ne doit pas depasser 100 caracteres")
    private String degree;

    @Min(value = 1950, message = "L'annee de graduation est invalide")
    @Max(value = 2100, message = "L'annee de graduation est invalide")
    private Integer graduationYear;
}
