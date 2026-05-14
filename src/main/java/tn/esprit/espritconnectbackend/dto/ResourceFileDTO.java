package tn.esprit.espritconnectbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ResourceFileDTO {
    private UUID id;
    private String name;
    private String mimeType;
    private String downloadUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
