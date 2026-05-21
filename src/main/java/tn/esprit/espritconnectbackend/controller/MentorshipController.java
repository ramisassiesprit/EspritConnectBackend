package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.MentoringRequestDTO;
import tn.esprit.espritconnectbackend.dto.MentoringSessionDTO;
import tn.esprit.espritconnectbackend.service.MentorshipService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/mentorship")
@RequiredArgsConstructor
public class MentorshipController {

    private final MentorshipService mentorshipService;

    @PostMapping("/requests")
    public ResponseEntity<MentoringRequestDTO> createRequest(@RequestBody MentoringRequestDTO requestDTO) {
        return ResponseEntity.ok(mentorshipService.createRequest(requestDTO));
    }

    @PostMapping("/offers")
    public ResponseEntity<MentoringRequestDTO> createOffer(@RequestBody MentoringRequestDTO requestDTO) {
        return ResponseEntity.ok(mentorshipService.createOffer(requestDTO));
    }

    @PutMapping("/requests/{id}/status")
    public ResponseEntity<MentoringRequestDTO> updateStatus(
            @PathVariable UUID id, 
            @RequestParam String status) {
        return ResponseEntity.ok(mentorshipService.updateRequestStatus(id, status));
    }

    @GetMapping("/requests/received")
    public ResponseEntity<List<MentoringRequestDTO>> getReceivedRequests() {
        return ResponseEntity.ok(mentorshipService.getMyReceivedRequests());
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<List<MentoringRequestDTO>> getSentRequests() {
        return ResponseEntity.ok(mentorshipService.getMySentRequests());
    }

    @PostMapping("/sessions")
    public ResponseEntity<MentoringSessionDTO> scheduleSession(@RequestBody MentoringSessionDTO sessionDTO) {
        return ResponseEntity.ok(mentorshipService.scheduleSession(sessionDTO));
    }

    @GetMapping("/requests/{id}/sessions")
    public ResponseEntity<List<MentoringSessionDTO>> getSessions(@PathVariable UUID id) {
        return ResponseEntity.ok(mentorshipService.getSessionsByRequest(id));
    }
}
