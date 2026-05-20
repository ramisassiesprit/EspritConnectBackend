package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.EventAdminStatsDTO;
import tn.esprit.espritconnectbackend.dto.EventDTO;
import tn.esprit.espritconnectbackend.dto.EventRegistrationDTO;
import tn.esprit.espritconnectbackend.entities.Event;
import tn.esprit.espritconnectbackend.entities.EventRegistration;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.*;
import tn.esprit.espritconnectbackend.entities.Badge;
import tn.esprit.espritconnectbackend.entities.UserBadge;
import tn.esprit.espritconnectbackend.repositories.BadgeRepository;
import tn.esprit.espritconnectbackend.repositories.UserBadgeRepository;
import tn.esprit.espritconnectbackend.repositories.EventRegistrationRepository;
import tn.esprit.espritconnectbackend.repositories.EventRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import tn.esprit.espritconnectbackend.service.Auth.EmailService;

import java.time.LocalDateTime;
import java.util.*;
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
    private final EmailService emailService;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    private User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private void notifyAllUsersOfEvent(Event event) {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            try {
                notificationService.createNotification(
                    user,
                    "Nouvel événement !",
                    "L'événement \"" + event.getTitle() + "\" a été publié. N'hésitez pas à y participer !",
                    NotificationType.EVENT_REMINDER, 
                    "EVENT",
                    event.getId()
                );
            } catch (Exception e) {
                log.error("Failed to create in-app notification for user: " + user.getEmail(), e);
            }
            
            try {
                new Thread(() -> {
                    try {
                        emailService.sendEventNotificationEmail(
                            user.getEmail(),
                            event.getTitle(),
                            event.getDescription(),
                            event.getStartAt() != null ? event.getStartAt().toString() : "",
                            event.getLocation(),
                            event.getCreator() != null ? (event.getCreator().getFirstName() + " " + event.getCreator().getLastName()) : "Esprit Connect"
                        );
                    } catch (Exception e) {
                        log.error("Failed to send email to user: " + user.getEmail(), e);
                    }
                }).start();
            } catch (Exception e) {
                log.error("Failed to start email thread for user: " + user.getEmail(), e);
            }
        }
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
        event.setTags(eventDTO.getTags());
        event.setEventType(eventDTO.getEventType());
        event.setCreator(creator);
        
        if (creator.getRole() == UserRole.ADMIN) {
            event.setStatus(EventStatus.UPCOMING);
        } else {
            event.setStatus(EventStatus.PENDING);
        }
        
        Event savedEvent = eventRepository.save(event);
        auditService.logAction("CREATE_EVENT", "EVENT", savedEvent.getId(), "Événement créé : " + savedEvent.getTitle());
        
        if (savedEvent.getStatus() == EventStatus.UPCOMING) {
            notifyAllUsersOfEvent(savedEvent);
        }
        
        return mapToDTO(savedEvent);
    }

    @Override
    @Transactional
    public EventDTO updateEvent(UUID eventId, EventDTO eventDTO) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        User currentUser = getCurrentUserEntity();
        if (!event.getCreator().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Seul le créateur ou un administrateur peut modifier l'événement");
        }
        
        event.setTitle(eventDTO.getTitle());
        event.setDescription(eventDTO.getDescription());
        event.setStartAt(eventDTO.getStartAt());
        event.setEndAt(eventDTO.getEndAt());
        event.setLocation(eventDTO.getLocation());
        event.setCoverUrl(eventDTO.getCoverUrl());
        event.setCapacity(eventDTO.getCapacity());
        event.setTags(eventDTO.getTags());
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
        if (!event.getCreator().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Seul le créateur ou un administrateur peut supprimer l'événement");
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
        User currentUser = getCurrentUserEntity();
        if (currentUser.getRole() == UserRole.ADMIN) {
            return eventRepository.findAll().stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } else {
            return eventRepository.findAll().stream()
                    .filter(event -> event.getStatus() == EventStatus.UPCOMING 
                            || event.getStatus() == EventStatus.PUBLISHED 
                            || event.getStatus() == EventStatus.COMPLETED
                            || event.getCreator().getId().equals(currentUser.getId()))
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public EventDTO approveEvent(UUID eventId) {
        User currentUser = getCurrentUserEntity();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Seul l'administrateur peut approuver un événement");
        }
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        if (event.getStatus() != EventStatus.PENDING) {
            throw new RuntimeException("L'événement n'est pas en attente d'approbation");
        }
        
        event.setStatus(EventStatus.UPCOMING);
        Event savedEvent = eventRepository.save(event);
        
        auditService.logAction("APPROVE_EVENT", "EVENT", eventId, "Événement approuvé par l'admin");
        
        notifyAllUsersOfEvent(savedEvent);
        
        return mapToDTO(savedEvent);
    }

    @Override
    @Transactional
    public EventDTO rejectEvent(UUID eventId) {
        User currentUser = getCurrentUserEntity();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Seul l'administrateur peut rejeter un événement");
        }
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        if (event.getStatus() != EventStatus.PENDING) {
            throw new RuntimeException("L'événement n'est pas en attente d'approbation");
        }
        
        event.setStatus(EventStatus.CANCELLED);
        Event savedEvent = eventRepository.save(event);
        
        auditService.logAction("REJECT_EVENT", "EVENT", eventId, "Événement rejeté par l'admin");
        
        return mapToDTO(savedEvent);
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
            if (Boolean.TRUE.equals(event.getWaitlistEnabled())) {
                EventRegistration registration = new EventRegistration();
                registration.setEvent(event);
                registration.setUser(user);
                registration.setStatus(RegistrationStatus.WAITLISTED);
                
                EventRegistration savedRegistration = registrationRepository.save(registration);
                
                // Notify user of waitlist
                notificationService.createNotification(
                    user,
                    "Inscrit sur liste d'attente",
                    "L'événement \"" + event.getTitle() + "\" est complet. Vous avez été ajouté à la liste d'attente.",
                    NotificationType.EVENT_REMINDER, 
                    "EVENT",
                    event.getId()
                );
                
                return mapToRegistrationDTO(savedRegistration);
            } else {
                throw new RuntimeException("L'événement est complet");
            }
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
        
        RegistrationStatus oldStatus = registration.getStatus();
        registrationRepository.delete(registration);
        
        if (oldStatus == RegistrationStatus.REGISTERED) {
            // Update count
            event.setRegisteredCount(event.getRegisteredCount() - 1);
            eventRepository.save(event);
            
            // Check waitlist auto-promotion
            if (Boolean.TRUE.equals(event.getWaitlistEnabled())) {
                List<EventRegistration> waitlist = registrationRepository.findByEventAndStatusOrderByRegisteredAtAsc(event, RegistrationStatus.WAITLISTED);
                if (!waitlist.isEmpty()) {
                    EventRegistration promotedRegistration = waitlist.get(0);
                    promotedRegistration.setStatus(RegistrationStatus.REGISTERED);
                    registrationRepository.save(promotedRegistration);
                    
                    // Increment count back
                    event.setRegisteredCount(event.getRegisteredCount() + 1);
                    eventRepository.save(event);
                    
                    // Notify promoted user in-app
                    notificationService.createNotification(
                        promotedRegistration.getUser(),
                        "🎉 Inscription validée !",
                        "Bonne nouvelle ! Une place s'est libérée et vous avez été inscrit d'office à l'événement : " + event.getTitle(),
                        NotificationType.EVENT_REMINDER, 
                        "EVENT",
                        event.getId()
                    );
                    
                    // Send notification email in background thread
                    try {
                        new Thread(() -> {
                            try {
                                emailService.sendEventNotificationEmail(
                                    promotedRegistration.getUser().getEmail(),
                                    "Inscription validée : " + event.getTitle(),
                                    "Une place s'est libérée ! Vous êtes inscrit(e) d'office.",
                                    event.getStartAt() != null ? event.getStartAt().toString() : "",
                                    event.getLocation(),
                                    event.getCreator() != null ? (event.getCreator().getFirstName() + " " + event.getCreator().getLastName()) : "Esprit Connect"
                                );
                            } catch (Exception e) {
                                log.error("Failed to send email to user: " + promotedRegistration.getUser().getEmail(), e);
                            }
                        }).start();
                    } catch (Exception e) {
                        log.error("Failed to start email thread", e);
                    }
                }
            }
        }
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

    @Override
    @Transactional(readOnly = true)
    public List<EventDTO> getRecommendedEvents() {
        User currentUser = getCurrentUserEntity();
        List<Event> upcomingEvents = eventRepository.findAll().stream()
                .filter(e -> e.getStatus() == EventStatus.UPCOMING || e.getStatus() == EventStatus.PUBLISHED)
                .toList();
        
        return upcomingEvents.stream()
                .map(event -> {
                    int score = 0;
                    
                    if (currentUser.getEspritProfile() != null && currentUser.getEspritProfile().getFieldOfStudy() != null) {
                        String field = currentUser.getEspritProfile().getFieldOfStudy().toLowerCase();
                        if (event.getTitle().toLowerCase().contains(field) 
                                || (event.getDescription() != null && event.getDescription().toLowerCase().contains(field))
                                || (event.getTags() != null && event.getTags().toLowerCase().contains(field))) {
                            score += 3;
                        }
                    }
                    
                    if (currentUser.getSkills() != null && !currentUser.getSkills().isEmpty()) {
                        for (var skill : currentUser.getSkills()) {
                            String skillName = skill.getName().toLowerCase();
                            if (event.getTitle().toLowerCase().contains(skillName)
                                    || (event.getDescription() != null && event.getDescription().toLowerCase().contains(skillName))
                                     || (event.getTags() != null && event.getTags().toLowerCase().contains(skillName))) {
                                score += 2;
                            }
                        }
                    }
                    
                    final int finalScore = score;
                    return new Object() {
                        final Event evt = event;
                        final int scr = finalScore;
                    };
                })
                .sorted((o1, o2) -> Integer.compare(o2.scr, o1.scr))
                .map(o -> mapToDTO(o.evt))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRegistrationDTO checkInUser(UUID eventId, UUID userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        User currentUser = getCurrentUserEntity();
        if (!event.getCreator().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Seul l'organisateur ou un admin peut effectuer le check-in");
        }
        
        User attendee = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        EventRegistration registration = registrationRepository.findByEventAndUser(event, attendee)
                .orElseThrow(() -> new RuntimeException("L'étudiant n'est pas inscrit à cet événement"));
        
        registration.setStatus(RegistrationStatus.ATTENDED);
        registration.setCheckedInAt(LocalDateTime.now());
        
        EventRegistration saved = registrationRepository.save(registration);
        auditService.logAction("CHECK_IN_EVENT", "EVENT", eventId, "Présence validée pour : " + attendee.getEmail());
        
        return mapToRegistrationDTO(saved);
    }

    @Override
    @Transactional
    public EventRegistrationDTO checkInUserByRegistrationId(UUID registrationId) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));
        
        Event event = registration.getEvent();
        User currentUser = getCurrentUserEntity();
        if (!event.getCreator().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Seul l'organisateur ou un admin peut effectuer le check-in");
        }
        
        registration.setStatus(RegistrationStatus.ATTENDED);
        registration.setCheckedInAt(LocalDateTime.now());
        
        EventRegistration saved = registrationRepository.save(registration);
        auditService.logAction("CHECK_IN_EVENT_QR", "EVENT", event.getId(), "Présence validée par QR pour : " + registration.getUser().getEmail());
        
        return mapToRegistrationDTO(saved);
    }

    @Override
    @Transactional
    public EventRegistrationDTO submitFeedback(UUID eventId, Integer rating, String comment) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        User currentUser = getCurrentUserEntity();
        EventRegistration registration = registrationRepository.findByEventAndUser(event, currentUser)
                .orElseThrow(() -> new RuntimeException("Vous n'êtes pas inscrit à cet événement"));
        
        if (registration.getStatus() != RegistrationStatus.ATTENDED && registration.getStatus() != RegistrationStatus.REGISTERED) {
            throw new RuntimeException("Vous devez avoir participé ou être inscrit pour donner votre avis");
        }
        
        registration.setFeedbackRating(rating);
        registration.setFeedbackComment(comment);
        
        EventRegistration saved = registrationRepository.save(registration);
        auditService.logAction("SUBMIT_FEEDBACK_EVENT", "EVENT", eventId, "Feedback soumis par : " + currentUser.getEmail());
        
        return mapToRegistrationDTO(saved);
    }

    @Override
    public List<EventRegistrationDTO> getEventFeedbacks(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        return registrationRepository.findByEvent(event).stream()
                .filter(r -> r.getFeedbackRating() != null)
                .map(this::mapToRegistrationDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRegistrationDTO declareWinner(UUID eventId, UUID userId, Integer rank) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        User currentUser = getCurrentUserEntity();
        if (!event.getCreator().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Seul l'organisateur ou un admin peut désigner les vainqueurs");
        }
        
        User winner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Vainqueur non trouvé"));
        
        EventRegistration registration = registrationRepository.findByEventAndUser(event, winner)
                .orElseThrow(() -> new RuntimeException("Ce vainqueur n'est pas inscrit à cet événement"));
        
        registration.setIsWinner(true);
        registration.setWinnerRank(rank);
        EventRegistration saved = registrationRepository.save(registration);
        
        try {
            String badgeName = "Champion : " + event.getTitle();
            String badgeDesc = "A remporté l'événement ou défi : " + event.getTitle() + " (Rang: " + (rank != null ? rank : "Gagnant") + ")";
            
            Badge badge = badgeRepository.findByName(badgeName).orElseGet(() -> {
                Badge newBadge = new Badge();
                newBadge.setName(badgeName);
                newBadge.setDescription(badgeDesc);
                newBadge.setType("EVENT_WINNER");
                newBadge.setIconUrl("assets/images/badges/champion.png");
                return badgeRepository.save(newBadge);
            });
            
            if (!userBadgeRepository.existsByUserAndBadge(winner, badge)) {
                UserBadge userBadge = new UserBadge();
                userBadge.setUser(winner);
                userBadge.setBadge(badge);
                userBadge.setEarnedAt(LocalDateTime.now());
                userBadgeRepository.save(userBadge);
                log.info("Badge de vainqueur '{}' attribué à {}", badgeName, winner.getEmail());
                
                notificationService.createNotification(
                    winner,
                    "🏆 Nouveau badge remporté !",
                    "Félicitations ! Vous avez été déclaré vainqueur de l'événement \"" + event.getTitle() + "\" et avez reçu le badge \"" + badgeName + "\".",
                    NotificationType.EVENT_REMINDER, 
                    "EVENT",
                    event.getId()
                );
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'attribution du badge vainqueur", e);
        }
        
        auditService.logAction("DECLARE_WINNER_EVENT", "EVENT", eventId, "Vainqueur désigné : " + winner.getEmail() + " (Rang: " + rank + ")");
        return mapToRegistrationDTO(saved);
    }

    @Override
    public List<EventRegistrationDTO> getEventWinners(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
        
        return registrationRepository.findByEvent(event).stream()
                .filter(EventRegistration::getIsWinner)
                .map(this::mapToRegistrationDTO)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public EventAdminStatsDTO getAdminEventStats() {
        log.info("Récupération des statistiques d'administration des événements");

        EventAdminStatsDTO stats = new EventAdminStatsDTO();

        // 1. Total d'événements
        long totalEvents = eventRepository.count();
        stats.setTotalEvents(totalEvents);

        // 2. Total des participants
        Long totalParticipants = registrationRepository.count();
        stats.setTotalParticipants(totalParticipants != null ? totalParticipants : 0);

        // 3. Utilisateurs éligibles (ETUDIANT + ALUMNI)
        long totalEligibleUsers = userRepository.countByRoleIn(
                List.of(UserRole.ETUDIANT, UserRole.ALUMNI)
        );
        stats.setTotalEligibleUsers(totalEligibleUsers);

        // 4. Taux de participation
        double participationRate = totalEligibleUsers > 0
                ? (totalParticipants / (double) totalEligibleUsers) * 100
                : 0;
        stats.setParticipationRate(participationRate);

        // 5. Taux de présence (checked-in / total registered)
        long totalCheckedIn = registrationRepository.countByCheckedInAtIsNotNull();
        double attendanceRate = totalParticipants > 0
                ? (totalCheckedIn / (double) totalParticipants) * 100
                : 0;
        stats.setAttendanceRate(attendanceRate);

        // 6. Score de satisfaction moyen
        Double avgFeedback = registrationRepository.getAverageFeedbackScore();
        stats.setAverageFeedbackScore(avgFeedback != null ? avgFeedback : 0.0);

        // 7. Total des gagnants
        long totalWinners = registrationRepository.countByIsWinnerTrue();
        stats.setTotalWinners(totalWinners);

        // 8. Top 3 événements populaires
        List<EventDTO> topEvents = eventRepository.findTop3ByOrderByRegisteredCountDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        stats.setTopPopularEvents(topEvents);

        // 9. Événements par statut
        Map<String, Long> eventsByStatus = new HashMap<>();
        for (EventStatus status : EventStatus.values()) {
            long count = eventRepository.countByStatus(status);
            eventsByStatus.put(status.name(), count);
        }
        stats.setEventsByStatus(eventsByStatus);

        // 10. Événements par type
        Map<String, Long> eventsByType = new HashMap<>();
        for (EventType type : EventType.values()) {
            long count = eventRepository.countByEventType(type);
            eventsByType.put(type.name(), count);
        }
        stats.setEventsByType(eventsByType);

        // 11. Inscriptions par mois
        Map<String, Long> registrationsByMonth = new LinkedHashMap<>();
        List<Object[]> monthData = eventRepository.countRegistrationsByMonth();
        for (Object[] row : monthData) {
            String month = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            registrationsByMonth.put(month, count);
        }
        stats.setRegistrationsByMonth(registrationsByMonth);

        log.info("Statistiques d'événements générées avec succès");
        return stats;
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
        dto.setTags(event.getTags());
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
        dto.setIsWinner(registration.getIsWinner());
        dto.setWinnerRank(registration.getWinnerRank());
        dto.setFeedbackRating(registration.getFeedbackRating());
        dto.setFeedbackComment(registration.getFeedbackComment());
        dto.setCheckedInAt(registration.getCheckedInAt());
        return dto;
    }
}
