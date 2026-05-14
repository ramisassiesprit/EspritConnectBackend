package tn.esprit.espritconnectbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ResourceFolderDTO {
    private UUID id;
    private String name;
    private String coverImageUrl;
    private String creatorName;
    private String creatorAvatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int itemsCount;
}
