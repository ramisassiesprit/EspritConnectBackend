package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class SkillDTO {
    private UUID id;

    @NotBlank(message = "Le nom de la competence est obligatoire")
    @Size(max = 100, message = "Le nom de la competence ne doit pas depasser 100 caracteres")
    private String name;
}
