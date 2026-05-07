package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.MentoringRequestDTO;
import tn.esprit.espritconnectbackend.dto.MentoringSessionDTO;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.MentoringRequest;
import tn.esprit.espritconnectbackend.entities.MentoringSession;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.MentoringStatus;
import tn.esprit.espritconnectbackend.entities.enums.NotificationType;
import tn.esprit.espritconnectbackend.repositories.MentoringRequestRepository;
import tn.esprit.espritconnectbackend.repositories.MentoringSessionRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorshipServiceImpl implements MentorshipService {

    private final MentoringRequestRepository requestRepository;
    private final MentoringSessionRepository sessionRepository;
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
    public MentoringRequestDTO createRequest(MentoringRequestDTO requestDTO) {
        User mentee = getCurrentUserEntity();
        
        // Use ID from nested mentor object if available, or mentee's input
        UUID mentorId = (requestDTO.getMentor() != null) ? requestDTO.getMentor().getId() : null;
        if (mentorId == null) {
            throw new RuntimeException("L'ID du mentor est obligatoire");
        }
        
        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor non trouvé"));
        
        if (!mentor.getIsMentor()) {
            throw new RuntimeException("Cet utilisateur n'est pas un mentor");
        }
        
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

    @Override
    @Transactional
    public MentoringRequestDTO updateRequestStatus(UUID requestId, String statusStr) {
        MentoringRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        User currentUser = getCurrentUserEntity();
        if (!request.getMentor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Seul le mentor peut répondre à cette demande");
        }
        
        MentoringStatus status = MentoringStatus.valueOf(statusStr.toUpperCase());
        request.setStatus(status);
        
        MentoringRequest updatedRequest = requestRepository.save(request);
        auditService.logAction("UPDATE_MENTORING_STATUS", "MENTORSHIP", requestId, "Statut de mentorat : " + status);
        
        // Notify mentee
        notificationService.createNotification(
            request.getMentee(),
            "Réponse mentorat",
            request.getMentor().getFirstName() + " a " + status.toString().toLowerCase() + " votre demande.",
            NotificationType.MENTORING_ACCEPTED,
            "MENTORSHIP_REQUEST",
            request.getId()
        );
        
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
