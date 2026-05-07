package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.GroupPrivacy;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GroupDTO {
    private UUID id;
    private String groupName;
    private String description;
    private String website;
    private String logoUrl;
    private String bannerUrl;
    private GroupPrivacy privacy;
    private Boolean tagging;
    private String labels;
    private Integer membersCount;
    private UUID creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
