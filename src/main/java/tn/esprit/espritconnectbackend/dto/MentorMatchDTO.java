package tn.esprit.espritconnectbackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class MentorMatchDTO {
    private UserDTO user;
    private EspritProfileDTO espritProfile;
    private double matchPercentage;
    private List<String> matchedSignals;
}