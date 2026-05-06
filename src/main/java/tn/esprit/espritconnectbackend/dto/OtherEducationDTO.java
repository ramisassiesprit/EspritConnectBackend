package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class OtherEducationDTO {
    private UUID id;
    private String institutionName;
    private String degree;
    private Integer graduationYear;
}
