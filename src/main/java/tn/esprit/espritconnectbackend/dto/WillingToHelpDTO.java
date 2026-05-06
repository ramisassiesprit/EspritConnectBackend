package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class WillingToHelpDTO {
    private UUID id;
    private String offering;
    private String seeking;
}
