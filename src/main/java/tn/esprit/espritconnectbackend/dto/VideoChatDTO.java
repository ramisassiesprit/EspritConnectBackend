package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class VideoChatDTO {
    private String topic;
    private String message;
    private String date;
    private String meetLink;
    private UUID receiverId;
}