package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.recommendation.MentorRecommendationDto;
import tn.esprit.espritconnectbackend.dto.recommendation.RecommendationResponseDto;

import java.util.List;
import java.util.UUID;

public interface RecommendationService {
    RecommendationResponseDto getRecommendationsForUser(UUID userId);
    List<MentorRecommendationDto> getRecommendedCandidatesForJob(UUID jobId);
    List<MentorRecommendationDto> getRecommendationsForCompany(UUID companyUserId);
}
