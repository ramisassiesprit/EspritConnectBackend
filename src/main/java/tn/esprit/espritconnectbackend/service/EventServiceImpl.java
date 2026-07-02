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
        applyAllowedEventFields(event, eventDTO, true);
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

        applyAllowedEventFields(event, eventDTO, false);
        if (currentUser.getRole() == UserRole.ADMIN && eventDTO.getStatus() != null) {
            event.setStatus(eventDTO.getStatus());
        }

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

        // Récupérer IDs des événements où l'utilisateur est déjà inscrit
        Set<UUID> registeredEventIds = registrationRepository.findByUser(currentUser)
                .stream()
                .map(r -> r.getEvent().getId())
                .collect(Collectors.toSet());

        // Récupérer candidats : événements approuvés/publies/à venir
        List<Event> candidates = eventRepository.findAll().stream()
                .filter(e -> e.getStatus() == EventStatus.UPCOMING || e.getStatus() == EventStatus.PUBLISHED)
                .filter(e -> !e.getCreator().getId().equals(currentUser.getId()))
                .filter(e -> !registeredEventIds.contains(e.getId()))
                .collect(Collectors.toList());

        // Préparer profils utilisateur (tags, skills, field)
        Set<String> userSkillTokens = tokenizeCollection(
                currentUser.getSkills() == null ? List.of() :
                        currentUser.getSkills().stream().map(s -> s.getName()).collect(Collectors.toList())
        );
        Set<String> userTags = new java.util.HashSet<>();
        if (currentUser.getEspritProfile() != null && currentUser.getEspritProfile().getFieldOfStudy() != null) {
            userTags.addAll(tokenizeString(currentUser.getEspritProfile().getFieldOfStudy()));
        }

        double wTag = 0.35;
        double wField = 0.25;
        double wSkill = 0.20;
        double wPopularity = 0.10;
        double wRecency = 0.10;

        long nowEpochDays = java.time.LocalDate.now().toEpochDay();

        List<ScoredEvent> scored = candidates.stream()
                .map(e -> {
                    // tokens from event (tags + title + description)
                    Set<String> eventTokens = new java.util.HashSet<>();
                    if (e.getTags() != null) eventTokens.addAll(tokenizeString(e.getTags()));
                    eventTokens.addAll(tokenizeString(e.getTitle()));
                    if (e.getDescription() != null) eventTokens.addAll(tokenizeString(e.getDescription()));

                    double tagScore = jaccard(userTags, eventTokens); // match on field + tags
                    double fieldScore = 0.0;
                    if (currentUser.getEspritProfile() != null && currentUser.getEspritProfile().getFieldOfStudy() != null) {
                        String field = currentUser.getEspritProfile().getFieldOfStudy().toLowerCase();
                        if (containsToken(eventTokens, field)) fieldScore = 1.0;
                    }

                    double skillScore = 0.0;
                    if (!userSkillTokens.isEmpty()) {
                        Set<String> intersection = new java.util.HashSet<>(userSkillTokens);
                        intersection.retainAll(eventTokens);
                        skillScore = (double) intersection.size() / Math.max(userSkillTokens.size(), 1);
                    }

                    // popularity normalized: log scale
                    double popularity = Math.log(1 + Math.max(0, e.getRegisteredCount() == null ? 0 : e.getRegisteredCount()));
                    // normalize popularity against an expected max (e.g., 10) to keep in [0,1]
                    double popularityNorm = Math.tanh(popularity / 3.0); // smooth normalization

                    // recency: days since creation
                    long daysSince = e.getCreatedAt() == null ? 365 : java.time.Duration.between(e.getCreatedAt(), LocalDateTime.now()).toDays();
                    double recency = Math.exp(- (double) daysSince / 30.0); // 30-day half-life-ish

                    double score = wTag * tagScore + wField * fieldScore + wSkill * skillScore
                            + wPopularity * popularityNorm + wRecency * recency;

                    return new ScoredEvent(e, score);
                })
                .sorted((s1, s2) -> Double.compare(s2.score, s1.score))
                .limit(20) // limiter le nombre de recommandations
                .collect(Collectors.toList());

        return scored.stream().map(se -> {
            EventDTO dto = mapToDTO(se.event);
            dto.setMatchScore(se.score);
            return dto;
        }).collect(Collectors.toList());
    }

    // Helper container
    private static class ScoredEvent {
        final Event event;
        final double score;
        ScoredEvent(Event event, double score) { this.event = event; this.score = score; }
    }

    // Tokenize helpers
    private static Set<String> tokenizeString(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String norm = text.toLowerCase().replaceAll("[^a-z0-9\\s,]", " ");
        String[] parts = norm.split("[,\\s]+");
        return Arrays.stream(parts)
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private static Set<String> tokenizeCollection(Collection<String> items) {
        Set<String> out = new java.util.HashSet<>();
        if (items == null) return out;
        for (String it : items) {
            if (it != null) out.addAll(tokenizeString(it));
        }
        return out;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if ((a == null || a.isEmpty()) && (b == null || b.isEmpty())) return 0.0;
        if (a == null) a = Collections.emptySet();
        if (b == null) b = Collections.emptySet();
        Set<String> inter = new java.util.HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new java.util.HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    private static boolean containsToken(Set<String> tokens, String value) {
        if (value == null || value.isBlank()) return false;
        String v = value.toLowerCase().trim();
        return tokens.stream().anyMatch(t -> t.equals(v) || t.contains(v) || v.contains(t));
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
    private String normalizeTags(String rawTags) {
        if (rawTags == null) return null;
        // split on comma or semicolon or whitespace, keep tokens of length>0
        String[] parts = rawTags.toLowerCase()
                .replace(';', ',')
                .split(",");
        // trim, remove empties, remove duplicates, enforce max tag length and max count
        Set<String> set = new LinkedHashSet<>();
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            // optionally remove non-alphanum except dash/underscore
            t = t.replaceAll("[^a-z0-9\\-\\_\\s]", "").trim();
            if (t.isEmpty()) continue;
            if (t.length() > 50) t = t.substring(0, 50); // cap length
            set.add(t);
            if (set.size() >= 20) break; // limit number of tags
        }
        return String.join(",", set);
    }
    private static final int MAX_COVER_URL_BYTES = 2 * 1024 * 1024; // 2 MB

    private void applyAllowedEventFields(Event event, EventDTO dto, boolean isCreate) {
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartAt(dto.getStartAt());
        event.setEndAt(dto.getEndAt());
        event.setLocation(dto.getLocation());

        // Validate coverUrl size to avoid MySQL max_allowed_packet errors
        String coverUrl = dto.getCoverUrl();
        if (coverUrl != null && coverUrl.startsWith("data:") && coverUrl.length() > MAX_COVER_URL_BYTES) {
            throw new IllegalArgumentException(
                "L'image de couverture est trop volumineuse (" +
                (coverUrl.length() / 1024) + " KB). La taille maximale autorisée est 2 MB. " +
                "Veuillez compresser l'image ou utiliser une URL externe."
            );
        }
        event.setCoverUrl(coverUrl);

        // capacity validation
        if (dto.getCapacity() != null) {
            int newCap = dto.getCapacity();
            if (newCap < 0) throw new IllegalArgumentException("Capacity must be >= 0");
            Integer currentRegistered = event.getRegisteredCount() == null ? 0 : event.getRegisteredCount();
            if (!isCreate && newCap < currentRegistered) {
                throw new IllegalArgumentException("La capacité ne peut pas être inférieure au nombre d'inscrits (" + currentRegistered + ")");
            }
            event.setCapacity(newCap);
        }

        // event type
        if (dto.getEventType() != null) {
            event.setEventType(dto.getEventType());
        }

        // TAGS — autoriser (nécessaire pour recommendation)
        if (dto.getTags() != null) {
            String normalized = normalizeTags(dto.getTags());
            event.setTags(normalized);
        }

        // Ne PAS appliquer d'autres champs comme waitlistEnabled, registeredCount, createdAt, etc.
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
