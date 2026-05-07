package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CommentDTO {
    private UUID id;
    private UUID postId;
    private UserDTO user;
    private UUID parentId;
    private List<CommentDTO> replies;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}