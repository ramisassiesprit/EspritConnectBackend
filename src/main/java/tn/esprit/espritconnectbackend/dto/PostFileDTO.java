package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PostFileDTO {
    private UUID id;
    private String name;
    private Long size;
    private String type;
    private String url;
}
