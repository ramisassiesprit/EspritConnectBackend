package tn.esprit.espritconnectbackend.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ResourceFolderDetailsDTO {
    private UUID id;
    private String name;
    private String coverImageUrl;
    private String creatorName;
    private String creatorAvatarUrl;
    private String createdAtLabel;
    private String updatedAtLabel;
    private List<ResourceFileDTO> files;
}
