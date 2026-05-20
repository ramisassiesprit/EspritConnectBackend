package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketCommentDTO {
    private UUID id;
    private String content;
    private Boolean isSolution;
    private UserDTO user;
    private UUID ticketPostId;
    private Integer upvotes;
    private Boolean hasUpvoted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
