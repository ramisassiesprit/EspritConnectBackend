package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.ReactionType;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PostReactionDTO {
    private UUID id;
    private UserDTO user;
    private UUID postId;
    private ReactionType reactionType;
    private LocalDateTime createdAt;
}
