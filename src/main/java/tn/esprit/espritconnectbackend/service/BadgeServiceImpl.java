package tn.esprit.espritconnectbackend.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.BadgeDTO;
import tn.esprit.espritconnectbackend.entities.Badge;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.UserBadge;
import tn.esprit.espritconnectbackend.entities.WillingToHelp;
import tn.esprit.espritconnectbackend.repositories.BadgeRepository;
import tn.esprit.espritconnectbackend.repositories.UserBadgeRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    public static final String BADGE_PROFILE_30 = "Profile Starter";
    public static final String BADGE_MENTOR = "Mentor Pro";

    @PostConstruct
    public void initBadges() {
        createBadgeIfNotFound(BADGE_PROFILE_30, "A complété au moins 30% de son profil", "PROFILE_COMPLETION");
        createBadgeIfNotFound(BADGE_MENTOR, "A proposé d'être mentor pour un étudiant", "MENTORING");
    }

    private void createBadgeIfNotFound(String name, String description, String type) {
        if (badgeRepository.findByName(name).isEmpty()) {
            Badge badge = new Badge();
            badge.setName(name);
            badge.setDescription(description);
            badge.setType(type);
            badgeRepository.save(badge);
            log.info("Badge créé : {}", name);
        }
    }
    @Override
    @Transactional(readOnly = true)
    public List<BadgeDTO> getUserBadges(UUID userId) {
        log.info("Récupération des badges pour l'utilisateur : {}", userId);

        List<UserBadge> userBadges = userBadgeRepository.findByUserId(userId);

        return userBadges.stream()
                .map(ub -> {
                    BadgeDTO dto = new BadgeDTO();
                    dto.setId(ub.getId());
                    dto.setName(ub.getBadge().getName());
                    dto.setDescription(ub.getBadge().getDescription());
                    dto.setType(ub.getBadge().getType());
                    dto.setEarnedAt(ub.getEarnedAt());
                    return dto;
                })
                .toList();
    }
    @Override
    @Transactional
    public void checkAndAwardBadges(User user) {
        log.info("Vérification des badges pour l'utilisateur : {}", user.getEmail());
        
        checkProfileCompletionBadge(user);
        checkMentoringBadge(user);
    }

    private void checkProfileCompletionBadge(User user) {
        Badge badge = badgeRepository.findByName(BADGE_PROFILE_30).orElse(null);
        if (badge == null || userBadgeRepository.existsByUserAndBadge(user, badge)) {
            return;
        }

        double completionPercentage = calculateProfileCompletion(user);
        if (completionPercentage >= 30.0) {
            awardBadge(user, badge);
        }
    }

    private void checkMentoringBadge(User user) {
        Badge badge = badgeRepository.findByName(BADGE_MENTOR).orElse(null);
        if (badge == null || userBadgeRepository.existsByUserAndBadge(user, badge)) {
            return;
        }

        List<WillingToHelp> helps = user.getWillingToHelps();
        if (helps != null) {
            boolean isMentor = helps.stream()
                    .anyMatch(h -> "Mentor a student".equalsIgnoreCase(h.getOfferMentor()));
            if (isMentor) {
                awardBadge(user, badge);
            }
        }
    }

    private void awardBadge(User user, Badge badge) {
        UserBadge userBadge = new UserBadge();
        userBadge.setUser(user);
        userBadge.setBadge(badge);
        userBadge.setEarnedAt(LocalDateTime.now());
        userBadgeRepository.save(userBadge);
        log.info("Badge '{}' attribué à l'utilisateur : {}", badge.getName(), user.getEmail());
    }

    private double calculateProfileCompletion(User user) {
        List<Object> fields = new ArrayList<>();
        // User basic fields
        fields.add(user.getFirstName());
        fields.add(user.getLastName());
        fields.add(user.getEmail());
        fields.add(user.getNumTel());
        fields.add(user.getAvatarUrl());
        fields.add(user.getBannerUrl());
        fields.add(user.getBio());
        fields.add(user.getLinkedinUrl());
        fields.add(user.getGithubUrl());
        fields.add(user.getFacebookUrl());
        fields.add(user.getCode());
        fields.add(user.getCompanyName());
        fields.add(user.getJobTitle());
        fields.add(user.getIndustry());
        fields.add(user.getJobFunction());

        // EspritProfile fields
        if (user.getEspritProfile() != null) {
            tn.esprit.espritconnectbackend.entities.EspritProfile profile = user.getEspritProfile();
            fields.add(profile.getFieldOfStudy());
            fields.add(profile.getDegree());
            fields.add(profile.getGraduationYear());
            fields.add(profile.getProgram());
            fields.add(profile.getInstitution());
        } else {
            // Add nulls to represent empty EspritProfile fields
            for (int i = 0; i < 6; i++) fields.add(null);
        }

        long filledFields = fields.stream()
                .filter(f -> f != null && !f.toString().trim().isEmpty())
                .count();

        return (double) filledFields / fields.size() * 100;
    }
}
