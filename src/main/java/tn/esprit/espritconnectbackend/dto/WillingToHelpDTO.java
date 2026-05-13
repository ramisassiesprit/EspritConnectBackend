package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class WillingToHelpDTO {
    private UUID id;
    private String offerHelp;
    private String seekHelp;
    private String offerMentor;
    private String seekMentor;
}
