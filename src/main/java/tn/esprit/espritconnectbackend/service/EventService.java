package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.EventDTO;
import tn.esprit.espritconnectbackend.dto.EventRegistrationDTO;

import java.util.List;
import java.util.UUID;

public interface EventService {
    EventDTO createEvent(EventDTO eventDTO);
    EventDTO updateEvent(UUID eventId, EventDTO eventDTO);
    void deleteEvent(UUID eventId);
    EventDTO getEventById(UUID eventId);
    List<EventDTO> getAllEvents();
    
    // Registration management
    EventRegistrationDTO registerToEvent(UUID eventId);
    void unregisterFromEvent(UUID eventId);
    List<EventRegistrationDTO> getEventRegistrations(UUID eventId);
    List<EventDTO> getUserEvents(UUID userId);
}
