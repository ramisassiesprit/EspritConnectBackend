package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminMailRequestDTO {

    @NotEmpty(message = "Au moins un destinataire est requis")
    private List<String> emails;

    @NotBlank(message = "Le sujet ne peut pas être vide")
    private String subject;

    @NotBlank(message = "Le message ne peut pas être vide")
    private String message;
}
