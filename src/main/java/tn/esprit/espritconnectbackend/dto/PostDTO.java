package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.PostType;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PostDTO {
    private UUID id;
    private UserDTO user;
    private String content;
    private String mediaUrl;
    private PostType postType;
    private Boolean isPinned;
    private Integer likesCount;
    private Integer commentsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID groupId;
    private java.util.List<String> images;
    private java.util.List<PostFileDTO> files;
    private Boolean liked;
}

