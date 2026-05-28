package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.ContractType;
import tn.esprit.espritconnectbackend.entities.enums.JobStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class JobOfferDTO {
    private UUID id;
    private UUID publisherId;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255, message = "Le titre ne doit pas depasser 255 caracteres")
    private String title;

    @Size(max = 10000, message = "La description est trop longue")
    private String description;

    @Size(max = 200, message = "Le nom de l'entreprise ne doit pas depasser 200 caracteres")
    private String company;

    @Size(max = 150, message = "L'industrie ne doit pas depasser 150 caracteres")
    private String industry;

    @Size(max = 255, message = "La localisation ne doit pas depasser 255 caracteres")
    private String location;

    private Double latitude;
    private Double longitude;

    private ContractType contractType;

    @Size(max = 100, message = "Le niveau d'experience ne doit pas depasser 100 caracteres")
    private String experienceLevel;

    @FutureOrPresent(message = "La date limite ne peut pas etre dans le passe")
    private LocalDate deadline;

    @Size(max = 1000, message = "Le lien de candidature est trop long")
    @Pattern(
            regexp = "^(https?://.*)?$",
            message = "Le lien de candidature doit commencer par http:// ou https://"
    )
    private String applyUrl;

    @Size(max = 1000, message = "Le lien de piece jointe est trop long")
    @Pattern(
            regexp = "^(https?://.*)?$",
            message = "Le lien de piece jointe doit commencer par http:// ou https://"
    )
    private String attachmentUrl;
    private List<String> targetFields;

    @Size(max = 1000, message = "Le lien de l'image est trop long")
    @Pattern(
            regexp = "^(https?://.*|/EspritConnect/.*)?$",
            message = "Le lien de l'image doit commencer par http://, https:// ou /EspritConnect/"
    )
    private String imageUrl;

    @NotNull(message = "Le statut est obligatoire")
    private JobStatus status;

    private String publisherName;
    private String publisherAvatarUrl;
    private String publisherJobTitle;
    private String publisherCompanyName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
