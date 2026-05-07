package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.EventDTO;
import tn.esprit.espritconnectbackend.dto.EventRegistrationDTO;
import tn.esprit.espritconnectbackend.entities.Event;
import tn.esprit.espritconnectbackend.entities.EventRegistration;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.EventStatus;
import tn.esprit.espritconnectbackend.entities.enums.NotificationType;
import tn.esprit.espritconnectbackend.entities.enums.RegistrationStatus;
import tn.esprit.espritconnectbackend.repositories.EventRegistrationRepository;
import tn.esprit.espritconnectbackend.repositories.EventRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    private User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @Override
    @Transactional
    public EventDTO createEvent(EventDTO eventDTO) {
        User creator = getCurrentUserEntity();
        
        Event event = new Event();
        event.setTitle(eventDTO.getTitle());
        event.setDescription(eventDTO.getDescription());
        event.setStartAt(eventDTO.getStartAt());
        event.setEndAt(eventDTO.getEndAt());
        event.setLocation(eventDTO.getLocation());
        event.setCoverUrl(eventDTO.getCoverUrl());
        event.setCapacity(eventDTO.getCapacity());
        event.setEventType(eventDTO.getEventType());
        event.setStatus(EventStatus.UPCOMING);
        event.setCreator(creator);
        
        Event savedEvent = eventRepository.save(event);
        auditService.logAction("CREATE_EVENT", "EVENT", savedEvent.getId(), "Événement créé : " + savedEvent.getTitle());
        
        return mapToDTO(savedEvent);
    }

    @Override
    @Transactional
    public EventDTO updateEvent(UUID eventId, EventDTO eventDTO) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        User currentUser = getCurrentUserEntity();
        if (!event.getCreator().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Seul le créateur peut modifier l'événement");
        }
        
        event.setTitle(eventDTO.getTitle());
        event.setDescription(eventDTO.getDescription());
        event.setStartAt(eventDTO.getStartAt());
        event.setEndAt(eventDTO.getEndAt());
        event.setLocation(eventDTO.getLocation());
        event.setCoverUrl(eventDTO.getCoverUrl());
        event.setCapacity(eventDTO.getCapacity());
        event.setEventType(eventDTO.getEventType());
        event.setStatus(eventDTO.getStatus());
        
        Event updatedEvent = eventRepository.save(event);
        auditService.logAction("UPDATE_EVENT", "EVENT", updatedEvent.getId(), "Événement mis à jour");
        
        return mapToDTO(updatedEvent);
    }

    @Override
    @Transactional
    public void deleteEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        User currentUser = getCurrentUserEntity();
        if (!event.getCreator().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Seul le créateur peut supprimer l'événement");
        }
        
        eventRepository.delete(event);
        auditService.logAction("DELETE_EVENT", "EVENT", eventId, "Événement supprimé");
    }

    @Override
    public EventDTO getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        return mapToDTO(event);
    }

    @Override
    public List<EventDTO> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRegistrationDTO registerToEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        User user = getCurrentUserEntity();
        
        if (registrationRepository.existsByEventAndUser(event, user)) {
            throw new RuntimeException("Vous êtes déjà inscrit à cet événement");
        }
        
        if (event.getCapacity() != null && event.getRegisteredCount() >= event.getCapacity()) {
            throw new RuntimeException("L'événement est complet");
        }
        
        EventRegistration registration = new EventRegistration();
        registration.setEvent(event);
        registration.setUser(user);
        registration.setStatus(RegistrationStatus.REGISTERED);
        
        EventRegistration savedRegistration = registrationRepository.save(registration);
        
        // Update count
        event.setRegisteredCount(event.getRegisteredCount() + 1);
        eventRepository.save(event);
        
        // Notify creator
        notificationService.createNotification(
            event.getCreator(),
            "Nouvelle inscription",
            user.getFirstName() + " s'est inscrit à votre événement : " + event.getTitle(),
            NotificationType.EVENT_REMINDER, 
            "EVENT",
            event.getId()
        );
        
        return mapToRegistrationDTO(savedRegistration);
    }

    @Override
    @Transactional
    public void unregisterFromEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        User user = getCurrentUserEntity();
        
        EventRegistration registration = registrationRepository.findByEventAndUser(event, user)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));
        
        registrationRepository.delete(registration);
        
        // Update count
        event.setRegisteredCount(event.getRegisteredCount() - 1);
        eventRepository.save(event);
    }

    @Override
    public List<EventRegistrationDTO> getEventRegistrations(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        return registrationRepository.findByEvent(event).stream()
                .map(this::mapToRegistrationDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventDTO> getUserEvents(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        return registrationRepository.findByUser(user).stream()
                .map(r -> mapToDTO(r.getEvent()))
                .collect(Collectors.toList());
    }

    private EventDTO mapToDTO(Event event) {
        EventDTO dto = new EventDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStartAt(event.getStartAt());
        dto.setEndAt(event.getEndAt());
        dto.setLocation(event.getLocation());
        dto.setCoverUrl(event.getCoverUrl());
        dto.setCapacity(event.getCapacity());
        dto.setRegisteredCount(event.getRegisteredCount());
        dto.setEventType(event.getEventType());
        dto.setStatus(event.getStatus());
        dto.setCreatorId(event.getCreator().getId());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setUpdatedAt(event.getUpdatedAt());
        return dto;
    }

    private EventRegistrationDTO mapToRegistrationDTO(EventRegistration registration) {
        EventRegistrationDTO dto = new EventRegistrationDTO();
        dto.setId(registration.getId());
        dto.setEventId(registration.getEvent().getId());
        dto.setUserId(registration.getUser().getId());
        dto.setUserFullName(registration.getUser().getFirstName() + " " + registration.getUser().getLastName());
        dto.setStatus(registration.getStatus());
        dto.setRegisteredAt(registration.getRegisteredAt());
        return dto;
    }
}
