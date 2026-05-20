package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class TicketPostDTO {
    private UUID id;
    private String title;
    private String content;
    private String category;
    private String status;
    private UserDTO user;
    private Integer upvotes;
    private Boolean hasUpvoted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TicketCommentDTO> comments;
}
