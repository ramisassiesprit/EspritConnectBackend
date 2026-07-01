package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.EspritProfileDTO;
import tn.esprit.espritconnectbackend.dto.MentorMatchDTO;
import tn.esprit.espritconnectbackend.dto.MentoringRequestDTO;
import tn.esprit.espritconnectbackend.dto.MentoringSessionDTO;
import tn.esprit.espritconnectbackend.dto.MentoringStatsDTO;
import tn.esprit.espritconnectbackend.dto.SessionFeedbackDTO;
import tn.esprit.espritconnectbackend.dto.TopMentorDTO;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.EspritProfile;
import tn.esprit.espritconnectbackend.entities.MentoringRequest;
import tn.esprit.espritconnectbackend.entities.MentoringSession;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.WillingToHelp;
import tn.esprit.espritconnectbackend.entities.enums.MentoringStatus;
import tn.esprit.espritconnectbackend.entities.enums.NotificationType;
import tn.esprit.espritconnectbackend.repositories.MentoringRequestRepository;
import tn.esprit.espritconnectbackend.repositories.MentoringSessionRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import tn.esprit.espritconnectbackend.repositories.WillingToHelpRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorshipServiceImpl implements MentorshipService {

    private static final List<MentoringStatus> ACTIVE_STATUSES = List.of(MentoringStatus.ACCEPTED);
    private static final int TOP_RECOMMENDATIONS_LIMIT = 10;

    private final MentoringRequestRepository requestRepository;
    private final MentoringSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final WillingToHelpRepository willingToHelpRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    private User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private void ensureNoActiveMentoring(User mentor, User mentee, UUID excludedRequestId) {
        boolean mentorBusy = excludedRequestId == null
                ? requestRepository.existsByMentorAndStatus(mentor, MentoringStatus.ACCEPTED)
                : requestRepository.existsByMentorAndStatusAndIdNot(mentor, MentoringStatus.ACCEPTED, excludedRequestId);

        if (mentorBusy) {
            throw new RuntimeException("Ce mentor a déjà un mentorat actif");
        }

        boolean menteeBusy = excludedRequestId == null
                ? requestRepository.existsByMenteeAndStatus(mentee, MentoringStatus.ACCEPTED)
                : requestRepository.existsByMenteeAndStatusAndIdNot(mentee, MentoringStatus.ACCEPTED, excludedRequestId);

        if (menteeBusy) {
            throw new RuntimeException("Cet utilisateur a déjà un mentorat actif");
        }
    }

    private void notifyMentoringStatus(MentoringRequest request, MentoringStatus status) {
        NotificationType notificationType = switch (status) {
            case ACCEPTED -> NotificationType.MENTORING_ACCEPTED;
            case REJECTED -> NotificationType.MENTORING_REJECTED;
            default -> NotificationType.SYSTEM;
        };

        String title = switch (status) {
            case ACCEPTED -> "Demande de mentorat acceptée";
            case REJECTED -> "Demande de mentorat refusée";
            case CANCELLED -> "Demande de mentorat annulée";
            case COMPLETED -> "Mentorat terminé";
            default -> "Mise à jour du mentorat";
        };

        String body = switch (status) {
            case ACCEPTED -> request.getMentor().getFirstName() + " a accepté votre demande de mentorat.";
            case REJECTED -> request.getMentor().getFirstName() + " a refusé votre demande de mentorat.";
            case CANCELLED -> "La demande de mentorat a été annulée.";
            case COMPLETED -> "Le mentorat a été marqué comme terminé.";
            default -> "Le statut de votre demande de mentorat a été mis à jour.";
        };

        notificationService.createNotification(
                request.getMentee(),
                title,
                body,
                notificationType,
                "MENTORSHIP_REQUEST",
                request.getId()
        );
    }

    @Override
    @Transactional
    public MentoringRequestDTO createRequest(MentoringRequestDTO requestDTO) {
        User mentee = getCurrentUserEntity();
        
        // Use ID from nested mentor object if available, or mentee's input
        UUID mentorId = (requestDTO.getMentor() != null) ? requestDTO.getMentor().getId() : null;
        if (mentorId == null) {
            throw new RuntimeException("L'ID du mentor est obligatoire");
        }
        
        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor non trouvé"));
        
        if (!mentor.getIsMentor() && !userOffersMentoring(mentor)) {
            throw new RuntimeException("Cet utilisateur n'est pas disponible pour le mentorat");
        }

        ensureNoActiveMentoring(mentor, mentee, null);
        
        MentoringRequest request = new MentoringRequest();
        request.setMentee(mentee);
        request.setMentor(mentor);
        request.setMessage(requestDTO.getMessage());
        request.setStatus(MentoringStatus.PENDING);
        
        MentoringRequest savedRequest = requestRepository.save(request);
        auditService.logAction("CREATE_MENTORING_REQUEST", "MENTORSHIP", savedRequest.getId(), "Demande de mentorat envoyée à " + mentor.getEmail());
        
        // Notify mentor
        notificationService.createNotification(
            mentor,
            "Nouvelle demande de mentorat",
            mentee.getFirstName() + " souhaite que vous soyez son mentor.",
            NotificationType.MENTORING_REQUEST, 
            "MENTORSHIP_REQUEST",
            savedRequest.getId()
        );
        
        return mapToRequestDTO(savedRequest);
    }

    private boolean userOffersMentoring(User user) {
        if (user.getWillingToHelps() == null) return false;
        return user.getWillingToHelps().stream()
                .anyMatch(h -> h.getOfferMentor() != null && !h.getOfferMentor().isBlank());
    }

    @Override
    @Transactional
    public MentoringRequestDTO createOffer(MentoringRequestDTO requestDTO) {
        User mentor = getCurrentUserEntity();

        if (!mentor.getIsMentor() && !userOffersMentoring(mentor)) {
            throw new RuntimeException("Vous devez être mentor pour offrir un mentorat ou indiquer que vous offrez du mentorat dans vos préférences");
        }

        UUID menteeId = (requestDTO.getMentee() != null) ? requestDTO.getMentee().getId() : null;
        if (menteeId == null) {
            throw new RuntimeException("L'ID du mentee est obligatoire pour une offre");
        }

        User mentee = userRepository.findById(menteeId)
                .orElseThrow(() -> new RuntimeException("Mentee non trouvé"));

        ensureNoActiveMentoring(mentor, mentee, null);

        MentoringRequest request = new MentoringRequest();
        request.setMentee(mentee);
        request.setMentor(mentor);
        request.setMessage(requestDTO.getMessage());
        request.setStatus(MentoringStatus.PENDING);

        MentoringRequest savedRequest = requestRepository.save(request);
        auditService.logAction("CREATE_MENTORING_OFFER", "MENTORSHIP", savedRequest.getId(), "Offre de mentorat envoyée à " + mentee.getEmail());

        // Notify mentee
        notificationService.createNotification(
                mentee,
                "Nouvelle offre de mentorat",
                mentor.getFirstName() + " vous propose d'être votre mentor.",
                NotificationType.MENTORING_REQUEST,
                "MENTORSHIP_OFFER",
                savedRequest.getId()
        );

        return mapToRequestDTO(savedRequest);
    }

    @Override
    @Transactional
    public MentoringRequestDTO updateRequestStatus(UUID requestId, String statusStr) {
        MentoringRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        User currentUser = getCurrentUserEntity();

        MentoringStatus status = MentoringStatus.valueOf(statusStr.toUpperCase());

        // Allow only the mentor to accept or reject a pending request.
        if (status == MentoringStatus.ACCEPTED || status == MentoringStatus.REJECTED) {
            if (!request.getMentor().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Seul le mentor peut répondre à cette demande");
            }
        }

        // Allow either party (mentor or mentee) to mark the mentorship as completed.
        if (status == MentoringStatus.ACCEPTED) {
            ensureNoActiveMentoring(request.getMentor(), request.getMentee(), requestId);
        }

        if (status == MentoringStatus.COMPLETED) {
            boolean isParticipant = request.getMentor().getId().equals(currentUser.getId()) || request.getMentee().getId().equals(currentUser.getId());
            if (!isParticipant) {
                throw new RuntimeException("Vous n'êtes pas autorisé à terminer ce mentorat");
            }
        }

        request.setStatus(status);
        
        MentoringRequest updatedRequest = requestRepository.save(request);
        auditService.logAction("UPDATE_MENTORING_STATUS", "MENTORSHIP", requestId, "Statut de mentorat : " + status);
        
        notifyMentoringStatus(updatedRequest, status);
        
        return mapToRequestDTO(updatedRequest);
    }

    @Override
    public List<MentoringRequestDTO> getMyReceivedRequests() {
        User user = getCurrentUserEntity();
        return requestRepository.findByMentor(user).stream()
                .map(this::mapToRequestDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MentoringRequestDTO> getMySentRequests() {
        User user = getCurrentUserEntity();
        return requestRepository.findByMentee(user).stream()
                .map(this::mapToRequestDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MentoringSessionDTO scheduleSession(MentoringSessionDTO sessionDTO) {
        MentoringRequest request = requestRepository.findById(sessionDTO.getRequestId())
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        User currentUser = getCurrentUserEntity();
        if (!request.getMentor().getId().equals(currentUser.getId()) && !request.getMentee().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'êtes pas partie prenante de ce mentorat");
        }
        
        MentoringSession session = new MentoringSession();
        session.setRequest(request);
        session.setSessionDate(sessionDTO.getSessionDate());
        session.setObjectives(sessionDTO.getObjectives());
        session.setNotes(sessionDTO.getNotes());
        
        MentoringSession savedSession = sessionRepository.save(session);
        
        // Notify the other party
        User otherParty = currentUser.getId().equals(request.getMentor().getId()) ? request.getMentee() : request.getMentor();
        notificationService.createNotification(
            otherParty,
            "Session de mentorat programmée",
            "Une nouvelle session a été programmée pour le " + session.getSessionDate(),
            NotificationType.EVENT_REMINDER,
            "MENTORSHIP_SESSION",
            savedSession.getId()
        );
        
        return mapToSessionDTO(savedSession);
    }

    @Override
    public List<MentoringSessionDTO> getSessionsByRequest(UUID requestId) {
        MentoringRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        return sessionRepository.findByRequest(request).stream()
                .map(this::mapToSessionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorMatchDTO> getRecommendedMentors(UUID userId) {
        User currentUser = userId != null
                ? userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"))
                : getCurrentUserEntity();
        EspritProfile seekerProfile = currentUser.getEspritProfile();

        if (seekerProfile == null) {
            return List.of();
        }

        return userRepository.findMentorCandidatesForMatching(currentUser.getId())
                .stream()
                .map(candidate -> buildMatch(seekerProfile, currentUser, candidate))
                .sorted(Comparator.comparingDouble(MentorMatchDTO::getMatchPercentage).reversed()
                        .thenComparing(match -> safe(match.getUser() == null ? null : match.getUser().getFirstName()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(match -> safe(match.getUser() == null ? null : match.getUser().getLastName()), String.CASE_INSENSITIVE_ORDER))
                .limit(TOP_RECOMMENDATIONS_LIMIT)
                .collect(Collectors.toList());
    }

    private MentorMatchDTO buildMatch(EspritProfile seekerProfile, User seeker, User candidate) {
        EspritProfile candidateProfile = candidate.getEspritProfile();
        List<String> matchedSignals = new ArrayList<>();

        double score = 0.0;
        double maxScore = 0.0;

        score += scoreTextCriterion(seekerProfile.getFieldOfStudy(), candidateProfile == null ? null : candidateProfile.getFieldOfStudy(), 30, "Field of study", matchedSignals);
        maxScore += isBlank(seekerProfile.getFieldOfStudy()) ? 0 : 30;

        score += scoreTextCriterion(seekerProfile.getProgram(), candidateProfile == null ? null : candidateProfile.getProgram(), 20, "Program", matchedSignals);
        maxScore += isBlank(seekerProfile.getProgram()) ? 0 : 20;

        score += scoreTextCriterion(seekerProfile.getDegree(), candidateProfile == null ? null : candidateProfile.getDegree(), 20, "Degree", matchedSignals);
        maxScore += isBlank(seekerProfile.getDegree()) ? 0 : 20;

        score += scoreTextCriterion(seekerProfile.getInstitution(), candidateProfile == null ? null : candidateProfile.getInstitution(), 10, "Institution", matchedSignals);
        maxScore += isBlank(seekerProfile.getInstitution()) ? 0 : 10;

        score += scoreYearCriterion(seekerProfile.getGraduationYear(), candidateProfile == null ? null : candidateProfile.getGraduationYear());
        maxScore += seekerProfile.getGraduationYear() == null ? 0 : 5;

        Set<String> seekerSkills = normalizeSkillSet(seeker.getSkills());
        if (!seekerSkills.isEmpty()) {
            Set<String> candidateSkills = normalizeSkillSet(candidate.getSkills());
            double skillSimilarity = overlapRatio(seekerSkills, candidateSkills);
            if (skillSimilarity > 0.0) {
                matchedSignals.add("Skills");
            }
            score += 15 * skillSimilarity;
            maxScore += 15;
        }

        double matchPercentage = maxScore == 0.0 ? 0.0 : (score / maxScore) * 100.0;

        MentorMatchDTO dto = new MentorMatchDTO();
        dto.setUser(mapToUserDTO(candidate));
        dto.setEspritProfile(candidateProfile == null ? null : mapToEspritProfileDTO(candidateProfile));
        dto.setMatchPercentage(Math.max(0.0, Math.min(100.0, matchPercentage)));
        dto.setMatchedSignals(matchedSignals);
        return dto;
    }

    private double scoreTextCriterion(String seekerValue, String candidateValue, int weight, String signalLabel, List<String> matchedSignals) {
        if (isBlank(seekerValue)) {
            return 0.0;
        }

        double similarity = textSimilarity(seekerValue, candidateValue);
        if (similarity > 0.0) {
            matchedSignals.add(signalLabel);
        }
        return weight * similarity;
    }

    private double scoreYearCriterion(Integer seekerYear, Integer candidateYear) {
        if (seekerYear == null || candidateYear == null) {
            return 0.0;
        }

        int difference = Math.abs(seekerYear - candidateYear);
        double similarity = Math.max(0.0, 1.0 - (difference / 10.0));
        return 5.0 * similarity;
    }

    private double textSimilarity(String seekerValue, String candidateValue) {
        if (isBlank(seekerValue) || isBlank(candidateValue)) {
            return 0.0;
        }

        String left = normalize(seekerValue);
        String right = normalize(candidateValue);

        if (left.equals(right)) {
            return 1.0;
        }

        if (left.contains(right) || right.contains(left)) {
            return 0.85;
        }

        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        if (intersection.isEmpty()) {
            return 0.0;
        }

        Set<String> union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);
        return (double) intersection.size() / union.size();
    }

    private Set<String> normalizeSkillSet(Set<?> skills) {
        if (skills == null || skills.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new HashSet<>();
        for (Object skill : skills) {
            if (skill instanceof tn.esprit.espritconnectbackend.entities.Skill typedSkill && !isBlank(typedSkill.getName())) {
                normalized.add(normalize(typedSkill.getName()));
            }
        }
        return normalized;
    }

    private double overlapRatio(Set<String> seekerSkills, Set<String> candidateSkills) {
        if (seekerSkills.isEmpty() || candidateSkills.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(seekerSkills);
        intersection.retainAll(candidateSkills);
        if (intersection.isEmpty()) {
            return 0.0;
        }

        Set<String> union = new HashSet<>(seekerSkills);
        union.addAll(candidateSkills);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String value) {
        if (isBlank(value)) {
            return Set.of();
        }

        return Arrays.stream(normalize(value).split("\\s+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    @Transactional(readOnly = true)
    public MentoringStatsDTO getStats() {
        long totalUsers = userRepository.count();

        long totalMentors = allRequests.stream()
                .map(MentoringRequest::getMentor)
                .filter(u -> u != null && Boolean.TRUE.equals(u.getIsMentor()))
                .distinct()
                .count();

        long usersOfferingHelp = 0;
        long usersSeekingHelp = 0;
        long usersOfferingMentoring = 0;
        long usersSeekingMentoring = 0;

        Map<String, Long> offerHelpByOption = new HashMap<>();
        Map<String, Long> seekHelpByOption = new HashMap<>();
        Map<String, Long> offerMentoringByOption = new HashMap<>();
        Map<String, Long> seekMentoringByOption = new HashMap<>();

        List<WillingToHelp> allWilling = willingToHelpRepository.findAll();
        for (WillingToHelp w : allWilling) {
            if (!isBlank(w.getOfferHelp())) {
                usersOfferingHelp++;
                for (String opt : w.getOfferHelp().split(",")) {
                    String trimmed = opt.trim();
                    if (!trimmed.isEmpty()) offerHelpByOption.merge(trimmed, 1L, Long::sum);
                }
            }
            if (!isBlank(w.getSeekHelp())) {
                usersSeekingHelp++;
                for (String opt : w.getSeekHelp().split(",")) {
                    String trimmed = opt.trim();
                    if (!trimmed.isEmpty()) seekHelpByOption.merge(trimmed, 1L, Long::sum);
                }
            }
            if (!isBlank(w.getOfferMentor())) {
                usersOfferingMentoring++;
                for (String opt : w.getOfferMentor().split(",")) {
                    String trimmed = opt.trim();
                    if (!trimmed.isEmpty()) offerMentoringByOption.merge(trimmed, 1L, Long::sum);
                }
            }
            if (!isBlank(w.getSeekMentor())) {
                usersSeekingMentoring++;
                for (String opt : w.getSeekMentor().split(",")) {
                    String trimmed = opt.trim();
                    if (!trimmed.isEmpty()) seekMentoringByOption.merge(trimmed, 1L, Long::sum);
                }
            }
        }

        List<Object[]> statusCounts = requestRepository.countByStatusGrouped();
        Map<String, Long> requestsByStatus = new LinkedHashMap<>();
        long totalRequests = 0;
        for (Object[] row : statusCounts) {
            String status = ((MentoringStatus) row[0]).name();
            long count = (Long) row[1];
            requestsByStatus.put(status, count);
            totalRequests += count;
        }

        List<Object[]> fieldCounts = requestRepository.countByMenteeFieldOfStudy();
        Map<String, Long> requestsByFieldOfStudy = new LinkedHashMap<>();
        for (Object[] row : fieldCounts) {
            requestsByFieldOfStudy.put((String) row[0], (Long) row[1]);
        }

        List<MentoringRequest> allRequests = requestRepository.findAll();
        Map<String, Long> requestsByMonth = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        allRequests.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRequestedAt().format(monthFormatter),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> requestsByMonth.put(e.getKey(), e.getValue()));

        long totalSessions = sessionRepository.count();
        Double avgRating = sessionRepository.averageRating();

        List<MentoringSession> allSessions = sessionRepository.findAll();

        // ── New analytics (in-memory) ────────────────────────────────────────
        List<TopMentorDTO> topMentors = computeTopMentors(allRequests, allSessions);
        Map<String, Long> ratingDistribution = computeRatingDistribution(allSessions);
        List<SessionFeedbackDTO> recentFeedback = computeRecentFeedback(allSessions);
        Map<String, Long> requestsByGraduationYear = computeRequestsByGraduationYear(allRequests);
        Map<String, Long> requestsByIndustry = computeRequestsByIndustry(allRequests);
        Map<String, long[]> supplyVsDemand = computeSupplyVsDemand(
                offerHelpByOption, seekHelpByOption, offerMentoringByOption, seekMentoringByOption);

        return MentoringStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalMentors(totalMentors)
                .usersOfferingHelp(usersOfferingHelp)
                .usersSeekingHelp(usersSeekingHelp)
                .usersOfferingMentoring(usersOfferingMentoring)
                .usersSeekingMentoring(usersSeekingMentoring)
                .offerHelpPercentage(totalUsers > 0 ? roundTo1((double) usersOfferingHelp / totalUsers * 100) : 0)
                .seekHelpPercentage(totalUsers > 0 ? roundTo1((double) usersSeekingHelp / totalUsers * 100) : 0)
                .offerMentoringPercentage(totalUsers > 0 ? roundTo1((double) usersOfferingMentoring / totalUsers * 100) : 0)
                .seekMentoringPercentage(totalUsers > 0 ? roundTo1((double) usersSeekingMentoring / totalUsers * 100) : 0)
                .offerHelpByOption(sortByValueDesc(offerHelpByOption))
                .seekHelpByOption(sortByValueDesc(seekHelpByOption))
                .offerMentoringByOption(sortByValueDesc(offerMentoringByOption))
                .seekMentoringByOption(sortByValueDesc(seekMentoringByOption))
                .totalRequests(totalRequests)
                .pendingRequests(requestsByStatus.getOrDefault("PENDING", 0L))
                .acceptedRequests(requestsByStatus.getOrDefault("ACCEPTED", 0L))
                .rejectedRequests(requestsByStatus.getOrDefault("REJECTED", 0L))
                .completedRequests(requestsByStatus.getOrDefault("COMPLETED", 0L))
                .cancelledRequests(requestsByStatus.getOrDefault("CANCELLED", 0L))
                .requestsByStatus(requestsByStatus)
                .requestsByMonth(requestsByMonth)
                .requestsByFieldOfStudy(requestsByFieldOfStudy)
                .totalSessions(totalSessions)
                .averageSessionRating(avgRating != null ? roundTo1(avgRating) : null)
                .topMentors(topMentors)
                .ratingDistribution(ratingDistribution)
                .recentFeedback(recentFeedback)
                .requestsByGraduationYear(requestsByGraduationYear)
                .requestsByIndustry(requestsByIndustry)
                .supplyVsDemandByOption(supplyVsDemand)
                .build();
    }

    private double roundTo1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private Map<String, Long> sortByValueDesc(Map<String, Long> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    // ── Stats helper methods ───────────────────────────────────────────────────

    /** Top mentors ranked by completed sessions, then acceptance rate, then avg rating. */
    private List<TopMentorDTO> computeTopMentors(List<MentoringRequest> allRequests,
                                                 List<MentoringSession> allSessions) {
        // group sessions by mentor id for completed-session + rating aggregation
        Map<UUID, List<MentoringSession>> sessionsByMentor = allSessions.stream()
                .filter(s -> s.getRequest() != null && s.getRequest().getMentor() != null)
                .collect(Collectors.groupingBy(s -> s.getRequest().getMentor().getId()));

        // group requests received by mentor id
        Map<UUID, List<MentoringRequest>> requestsByMentor = allRequests.stream()
                .filter(r -> r.getMentor() != null)
                .collect(Collectors.groupingBy(r -> r.getMentor().getId()));

        Set<UUID> mentorIds = new HashSet<>();
        mentorIds.addAll(sessionsByMentor.keySet());
        mentorIds.addAll(requestsByMentor.keySet());

        List<TopMentorDTO> dtos = new ArrayList<>();
        for (UUID mentorId : mentorIds) {
            List<MentoringRequest> reqs = requestsByMentor.getOrDefault(mentorId, List.of());
            List<MentoringSession> sessions = sessionsByMentor.getOrDefault(mentorId, List.of());

            // need the mentor entity to read names — pull from the first request
            User mentor = reqs.stream().findFirst().map(MentoringRequest::getMentor)
                    .or(() -> sessions.stream().findFirst().map(s -> s.getRequest().getMentor()))
                    .orElse(null);
            if (mentor == null) continue;

            long accepted = reqs.stream().filter(r -> r.getStatus() == MentoringStatus.ACCEPTED
                    || r.getStatus() == MentoringStatus.COMPLETED).count();
            long completed = sessions.size();
            long totalReceived = reqs.size();
            double acceptanceRate = totalReceived > 0
                    ? roundTo1((double) accepted / totalReceived * 100) : 0;

            Double avgRating = sessions.stream()
                    .map(MentoringSession::getRating)
                    .filter(r -> r != null && r > 0)
                    .mapToInt(Integer::intValue)
                    .average().stream().boxed().findFirst().orElse(null);
            if (avgRating != null) avgRating = roundTo1(avgRating);

            dtos.add(TopMentorDTO.builder()
                    .firstName(mentor.getFirstName())
                    .lastName(mentor.getLastName())
                    .completedSessions(completed)
                    .acceptedRequests(accepted)
                    .totalReceived(totalReceived)
                    .acceptanceRate(acceptanceRate)
                    .avgRating(avgRating)
                    .build());
        }

        dtos.sort(Comparator.comparing(TopMentorDTO::getCompletedSessions).reversed()
                .thenComparing(Comparator.comparing(TopMentorDTO::getAcceptanceRate).reversed()));

        return dtos.stream().limit(8).collect(Collectors.toList());
    }

    /** Distribution of session ratings across 1–5 stars. Missing stars map to 0. */
    private Map<String, Long> computeRatingDistribution(List<MentoringSession> allSessions) {
        Map<String, Long> dist = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            dist.put(String.valueOf(star), 0L);
        }
        for (MentoringSession s : allSessions) {
            Integer r = s.getRating();
            if (r != null && r >= 1 && r <= 5) {
                String key = String.valueOf(r);
                dist.merge(key, 1L, Long::sum);
            }
        }
        return dist;
    }

    /** Most recent sessions that have feedback text, newest first (max 5). */
    private List<SessionFeedbackDTO> computeRecentFeedback(List<MentoringSession> allSessions) {
        return allSessions.stream()
                .filter(s -> s.getFeedback() != null && !s.getFeedback().trim().isEmpty())
                .filter(s -> s.getSessionDate() != null)
                .sorted(Comparator.comparing(MentoringSession::getSessionDate).reversed())
                .limit(5)
                .map(s -> SessionFeedbackDTO.builder()
                        .mentorName(fullName(s.getRequest().getMentor()))
                        .menteeName(fullName(s.getRequest().getMentee()))
                        .rating(s.getRating())
                        .feedback(s.getFeedback())
                        .sessionDate(s.getSessionDate())
                        .build())
                .collect(Collectors.toList());
    }

    /** Requests grouped by the mentee's graduation year (descending by count). */
    private Map<String, Long> computeRequestsByGraduationYear(List<MentoringRequest> allRequests) {
        Map<String, Long> map = new HashMap<>();
        for (MentoringRequest r : allRequests) {
            EspritProfile profile = r.getMentee() != null ? r.getMentee().getEspritProfile() : null;
            if (profile != null && profile.getGraduationYear() != null) {
                String key = String.valueOf(profile.getGraduationYear());
                map.merge(key, 1L, Long::sum);
            }
        }
        return sortByValueDesc(map);
    }

    /** Requests grouped by the mentor's industry (descending by count). */
    private Map<String, Long> computeRequestsByIndustry(List<MentoringRequest> allRequests) {
        Map<String, Long> map = new HashMap<>();
        for (MentoringRequest r : allRequests) {
            User mentor = r.getMentor();
            if (mentor != null && !isBlank(mentor.getIndustry())) {
                map.merge(mentor.getIndustry().trim(), 1L, Long::sum);
            }
        }
        return sortByValueDesc(map);
    }

    /**
     * For every option seen across the four willing-to-help buckets, build a
     * {@code [supply, demand]} pair where supply = offerHelp + offerMentor and
     * demand = seekHelp + seekMentor.
     */
    private Map<String, long[]> computeSupplyVsDemand(Map<String, Long> offerHelp,
                                                     Map<String, Long> seekHelp,
                                                     Map<String, Long> offerMentor,
                                                     Map<String, Long> seekMentor) {
        Set<String> options = new HashSet<>();
        options.addAll(offerHelp.keySet());
        options.addAll(offerMentor.keySet());
        options.addAll(seekHelp.keySet());
        options.addAll(seekMentor.keySet());

        Map<String, long[]> result = new LinkedHashMap<>();
        for (String option : options) {
            long supply = offerHelp.getOrDefault(option, 0L) + offerMentor.getOrDefault(option, 0L);
            long demand = seekHelp.getOrDefault(option, 0L) + seekMentor.getOrDefault(option, 0L);
            result.put(option, new long[]{supply, demand});
        }
        return result;
    }

    private String fullName(User user) {
        if (user == null) return "Unknown";
        String first = safe(user.getFirstName());
        String last = safe(user.getLastName());
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "Unknown" : name;
    }

    private EspritProfileDTO mapToEspritProfileDTO(EspritProfile profile) {
        EspritProfileDTO dto = new EspritProfileDTO();
        dto.setId(profile.getId());
        dto.setFieldOfStudy(profile.getFieldOfStudy());
        dto.setDegree(profile.getDegree());
        dto.setGraduationYear(profile.getGraduationYear());
        dto.setProgram(profile.getProgram());
        dto.setInstitution(profile.getInstitution());
        return dto;
    }

    private MentoringRequestDTO mapToRequestDTO(MentoringRequest request) {
        MentoringRequestDTO dto = new MentoringRequestDTO();
        dto.setId(request.getId());
        dto.setMentee(mapToUserDTO(request.getMentee()));
        dto.setMentor(mapToUserDTO(request.getMentor()));
        dto.setMessage(request.getMessage());
        dto.setStatus(request.getStatus());
        dto.setRequestedAt(request.getRequestedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        return dto;
    }

    private MentoringSessionDTO mapToSessionDTO(MentoringSession session) {
        MentoringSessionDTO dto = new MentoringSessionDTO();
        dto.setId(session.getId());
        dto.setRequestId(session.getRequest().getId());
        dto.setSessionDate(session.getSessionDate());
        dto.setObjectives(session.getObjectives());
        dto.setNotes(session.getNotes());
        dto.setRating(session.getRating());
        dto.setFeedback(session.getFeedback());
        dto.setCreatedAt(session.getCreatedAt());
        return dto;
    }

    private UserDTO mapToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setRole(user.getRole());
        return dto;
    }
}
