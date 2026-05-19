package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.EventDTO;
import tn.esprit.espritconnectbackend.dto.EventRegistrationDTO;
import tn.esprit.espritconnectbackend.service.EventService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventDTO> createEvent(@RequestBody EventDTO eventDTO) {
        return ResponseEntity.ok(eventService.createEvent(eventDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> updateEvent(@PathVariable UUID id, @RequestBody EventDTO eventDTO) {
        return ResponseEntity.ok(eventService.updateEvent(id, eventDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<EventRegistrationDTO> register(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.registerToEvent(id));
    }

    @DeleteMapping("/{id}/unregister")
    public ResponseEntity<Void> unregister(@PathVariable UUID id) {
        eventService.unregisterFromEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/registrations")
    public ResponseEntity<List<EventRegistrationDTO>> getRegistrations(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventRegistrations(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<EventDTO> approveEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.approveEvent(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<EventDTO> rejectEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.rejectEvent(id));
    }

    @GetMapping("/user/{userId}/registered")
    public ResponseEntity<List<EventDTO>> getUserEvents(@PathVariable UUID userId) {
        return ResponseEntity.ok(eventService.getUserEvents(userId));
    }

    @GetMapping("/recommended")
    public ResponseEntity<List<EventDTO>> getRecommendedEvents() {
        return ResponseEntity.ok(eventService.getRecommendedEvents());
    }

    @PostMapping("/{id}/registrations/{userId}/check-in")
    public ResponseEntity<EventRegistrationDTO> checkIn(@PathVariable UUID id, @PathVariable UUID userId) {
        return ResponseEntity.ok(eventService.checkInUser(id, userId));
    }

    @PostMapping("/registrations/{registrationId}/check-in")
    public ResponseEntity<EventRegistrationDTO> checkInByRegistration(@PathVariable UUID registrationId) {
        return ResponseEntity.ok(eventService.checkInUserByRegistrationId(registrationId));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<EventRegistrationDTO> submitFeedback(
            @PathVariable UUID id,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(eventService.submitFeedback(id, rating, comment));
    }

    @GetMapping("/{id}/feedbacks")
    public ResponseEntity<List<EventRegistrationDTO>> getFeedbacks(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventFeedbacks(id));
    }

    @PostMapping("/{id}/registrations/{userId}/winner")
    public ResponseEntity<EventRegistrationDTO> declareWinner(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestParam(required = false) Integer rank) {
        return ResponseEntity.ok(eventService.declareWinner(id, userId, rank));
    }

    @GetMapping("/{id}/winners")
    public ResponseEntity<List<EventRegistrationDTO>> getWinners(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventWinners(id));
    }
}
