package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.MentoringRequestDTO;
import tn.esprit.espritconnectbackend.dto.MentoringSessionDTO;

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
    
    // Session management
    MentoringSessionDTO scheduleSession(MentoringSessionDTO sessionDTO);
    List<MentoringSessionDTO> getSessionsByRequest(UUID requestId);
}
