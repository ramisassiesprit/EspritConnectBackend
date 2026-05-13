package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.BadgeDTO;
import tn.esprit.espritconnectbackend.service.BadgeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BadgeDTO>> getUserBadges(@PathVariable UUID userId) {
        List<BadgeDTO> badges = badgeService.getUserBadges(userId);
        return ResponseEntity.ok(badges);
    }
}