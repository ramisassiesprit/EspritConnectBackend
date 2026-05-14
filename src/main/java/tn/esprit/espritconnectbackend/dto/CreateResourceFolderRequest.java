package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateResourceFolderRequest {
    @NotBlank(message = "Le nom du dossier est obligatoire")
    @Size(max = 200, message = "Le nom du dossier ne doit pas depasser 200 caracteres")
    private String name;

    @Size(max = 500, message = "L'URL de couverture ne doit pas depasser 500 caracteres")
    private String coverImageUrl;
}
