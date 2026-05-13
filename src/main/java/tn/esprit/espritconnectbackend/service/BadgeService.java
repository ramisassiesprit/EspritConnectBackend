package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.BadgeDTO;
import tn.esprit.espritconnectbackend.entities.User;

import java.util.List;
import java.util.UUID;

public interface BadgeService {
    void checkAndAwardBadges(User user);
    List<BadgeDTO> getUserBadges(UUID userId);
}
