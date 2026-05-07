package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;

@Data
public class EspritProfileDTO {
    private UUID id;
    
    @NotBlank(message = "Le numéro d'étudiant est obligatoire")
    private String studentNumber;
    
    @NotBlank(message = "Le domaine d'étude est obligatoire")
    private String fieldOfStudy;
    
    private String degree;
    private Integer graduationYear;
    private String program;
    private String institution;
}
