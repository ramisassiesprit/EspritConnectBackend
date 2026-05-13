package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.entities.User;

public interface BadgeService {
    void checkAndAwardBadges(User user);
}
