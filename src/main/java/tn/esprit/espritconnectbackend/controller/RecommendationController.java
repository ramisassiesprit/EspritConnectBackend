package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import tn.esprit.espritconnectbackend.service.RecommendationService;

import java.util.UUID;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping("/user")
    public ResponseEntity<?> getUserRecommendations(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return ResponseEntity.ok(recommendationService.getRecommendationsForUser(user.getId()));
    }

    @GetMapping("/job/{jobId}/candidates")
    public ResponseEntity<?> getRecommendedCandidates(@PathVariable UUID jobId) {
        return ResponseEntity.ok(recommendationService.getRecommendedCandidatesForJob(jobId));
    }

    @GetMapping("/company")
    public ResponseEntity<?> getRecommendationsForCompany(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return ResponseEntity.ok(recommendationService.getRecommendationsForCompany(user.getId()));
    }
}
