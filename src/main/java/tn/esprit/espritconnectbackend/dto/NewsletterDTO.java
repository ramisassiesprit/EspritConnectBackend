package tn.esprit.espritconnectbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NewsletterDTO {
    private UUID id;
    private UUID senderId;
    private String subject;
    private String body;
    private String targetFilter;
    private Integer sentCount;
    private LocalDateTime sentAt;
}
