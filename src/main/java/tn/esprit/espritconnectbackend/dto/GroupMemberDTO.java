package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.GroupMemberRole;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GroupMemberDTO {
    private UUID id;
    private UUID groupId;
    private UUID userId;
    private String userFullName; // Helper field
    private GroupMemberRole role;
    private Boolean isManual;
    private LocalDateTime joinedAt;
    
    // User profile details
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Boolean isOnline;
    private String userRole;
    private String status;
}
