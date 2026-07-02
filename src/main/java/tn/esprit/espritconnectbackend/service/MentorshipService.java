package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.MentorMatchDTO;
import tn.esprit.espritconnectbackend.dto.MentoringRequestDTO;
import tn.esprit.espritconnectbackend.dto.MentoringSessionDTO;
import tn.esprit.espritconnectbackend.dto.MentoringStatsDTO;

import java.util.List;
import java.util.UUID;

public interface MentorshipService {
    // Request management
    MentoringRequestDTO createRequest(MentoringRequestDTO requestDTO);
    // Mentor-initiated offer: current authenticated user offers mentoring to another user
    MentoringRequestDTO createOffer(MentoringRequestDTO requestDTO);
    MentoringRequestDTO updateRequestStatus(UUID requestId, String status);
    List<MentoringRequestDTO> getMyReceivedRequests();
    List<MentoringRequestDTO> getMySentRequests();
    List<MentorMatchDTO> getRecommendedMentors(UUID userId);
    
    // Session management
    MentoringSessionDTO scheduleSession(MentoringSessionDTO sessionDTO);
    List<MentoringSessionDTO> getSessionsByRequest(UUID requestId);

    // Statistics
    MentoringStatsDTO getStats();
}
