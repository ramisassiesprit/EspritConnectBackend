package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.espritconnectbackend.dto.recommendation.*;
import tn.esprit.espritconnectbackend.entities.Event;
import tn.esprit.espritconnectbackend.entities.Group;
import tn.esprit.espritconnectbackend.entities.JobOffer;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.JobStatus;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.repositories.EventRepository;
import tn.esprit.espritconnectbackend.repositories.GroupRepository;
import tn.esprit.espritconnectbackend.repositories.JobOfferRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final UserRepository userRepository;
    private final JobOfferRepository jobOfferRepository;
    private final EventRepository eventRepository;
    private final GroupRepository groupRepository;

    @Override
    public RecommendationResponseDto getRecommendationsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String cvKeywords = user.getCvKeywords();
        String userContext = (user.getJobTitle() != null ? user.getJobTitle() : "") + " "
                + (user.getIndustry() != null ? user.getIndustry() : "") + " "
                + (cvKeywords != null ? cvKeywords : "");

        List<String> keywords = extractKeywords(userContext);
        log.debug("Keywords for user {}: {} keywords extracted", user.getEmail(), keywords.size());

        // --- Jobs ---
        List<JobRecommendationDto> recommendedJobs = jobOfferRepository.findAll().stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN)
                .map(job -> {
                    String target = safe(job.getTitle()) + " " + safe(job.getDescription()) + " " + safe(job.getTargetFieldsOfStudy());
                    double score = calculateScore(target, keywords);
                    return new AbstractMap.SimpleEntry<>(job, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> toJobDto(e.getKey()))
                .collect(Collectors.toList());

        // --- Events ---
        List<EventRecommendationDto> recommendedEvents = eventRepository.findAll().stream()
                .map(event -> {
                    String target = safe(event.getTitle()) + " " + safe(event.getDescription()) + " " + safe(event.getTags());
                    double score = calculateScore(target, keywords);
                    return new AbstractMap.SimpleEntry<>(event, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> toEventDto(e.getKey()))
                .collect(Collectors.toList());

        // --- Groups ---
        List<GroupRecommendationDto> recommendedGroups = groupRepository.findAll().stream()
                .map(group -> {
                    String target = safe(group.getGroupName()) + " " + safe(group.getDescription());
                    double score = calculateScore(target, keywords);
                    return new AbstractMap.SimpleEntry<>(group, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> toGroupDto(e.getKey()))
                .collect(Collectors.toList());

        // --- Mentors ---
        List<MentorRecommendationDto> recommendedMentors = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsMentor()) && !u.getId().equals(userId))
                .map(mentor -> {
                    String target = safe(mentor.getJobTitle()) + " " + safe(mentor.getIndustry()) + " " + safe(mentor.getBio());
                    double score = calculateScore(target, keywords);
                    return new AbstractMap.SimpleEntry<>(mentor, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> toMentorDto(e.getKey()))
                .collect(Collectors.toList());

        return RecommendationResponseDto.builder()
                .jobs(recommendedJobs)
                .events(recommendedEvents)
                .groups(recommendedGroups)
                .mentors(recommendedMentors)
                .build();
    }

    @Override
    public List<MentorRecommendationDto> getRecommendedCandidatesForJob(UUID jobId) {
        JobOffer job = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job Offer not found"));

        String jobContext = safe(job.getTitle()) + " " + safe(job.getDescription()) + " " + safe(job.getTargetFieldsOfStudy());
        List<String> keywords = extractKeywords(jobContext);

        return userRepository.findAll().stream()
                .filter(u -> (u.getRole() == UserRole.ETUDIANT || u.getRole() == UserRole.ALUMNI) && u.getRole() != UserRole.ADMIN)
                .map(user -> {
                    String target = safe(user.getCvKeywords()) + " " + safe(user.getJobTitle()) + " " + safe(user.getIndustry());
                    double score = calculateScore(target, keywords);
                    return new AbstractMap.SimpleEntry<>(user, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(20)
                .map(e -> toMentorDto(e.getKey()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MentorRecommendationDto> getRecommendationsForCompany(UUID companyUserId) {
        User company = userRepository.findById(companyUserId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        if (company.getRole() != UserRole.ENTREPRISE && company.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("User is not an Enterprise");
        }

        String companyContext = safe(company.getCompanyName()) + " " 
                              + safe(company.getIndustry()) + " " 
                              + safe(company.getBio()) + " "
                              + safe(company.getJobFunction());
                              
        List<String> keywords = extractKeywords(companyContext);

        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ETUDIANT || u.getRole() == UserRole.ALUMNI)
                .map(user -> {
                    String target = safe(user.getCvKeywords()) + " " 
                                  + safe(user.getJobTitle()) + " " 
                                  + safe(user.getIndustry()) + " "
                                  + safe(user.getBio());
                    double score = calculateScore(target, keywords);
                    return new AbstractMap.SimpleEntry<>(user, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(20)
                .map(e -> toMentorDto(e.getKey()))
                .collect(Collectors.toList());
    }

    // ─── Mappers ────────────────────────────────────────────────────────────────

    private JobRecommendationDto toJobDto(JobOffer job) {
        return JobRecommendationDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .description(job.getDescription() != null && job.getDescription().length() > 300
                        ? job.getDescription().substring(0, 300) + "..." : job.getDescription())
                .contractType(job.getContractType() != null ? job.getContractType().name() : null)
                .experienceLevel(job.getExperienceLevel())
                .deadline(job.getDeadline())
                .imageUrl(job.getImageUrl())
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .build();
    }

    private EventRecommendationDto toEventDto(Event event) {
        return EventRecommendationDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription() != null && event.getDescription().length() > 300
                        ? event.getDescription().substring(0, 300) + "..." : event.getDescription())
                .location(event.getLocation())
                .startAt(event.getStartAt())
                .endAt(event.getEndAt())
                .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                .tags(event.getTags())
                .coverUrl(event.getCoverUrl())
                .registeredCount(event.getRegisteredCount())
                .capacity(event.getCapacity())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .build();
    }

    private GroupRecommendationDto toGroupDto(Group group) {
        return GroupRecommendationDto.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .description(group.getDescription() != null && group.getDescription().length() > 300
                        ? group.getDescription().substring(0, 300) + "..." : group.getDescription())
                .logoUrl(group.getLogoUrl())
                .bannerUrl(group.getBannerUrl())
                .privacy(group.getPrivacy() != null ? group.getPrivacy().name() : null)
                .membersCount(group.getMembersCount() != null ? group.getMembersCount() : 0)
                .build();
    }

    private MentorRecommendationDto toMentorDto(User user) {
        return MentorRecommendationDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .jobTitle(user.getJobTitle())
                .companyName(user.getCompanyName())
                .industry(user.getIndustry())
                .bio(user.getBio())
                .build();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String safe(String s) {
        return s != null ? s : "";
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+"))
                .filter(word -> word.length() > 3)
                .distinct()
                .collect(Collectors.toList());
    }

    private double calculateScore(String targetText, List<String> keywords) {
        if (targetText == null || keywords.isEmpty()) {
            return 0.0;
        }
        String targetLower = targetText.toLowerCase();
        long matchCount = keywords.stream().filter(targetLower::contains).count();
        return (double) matchCount / keywords.size();
    }
}
