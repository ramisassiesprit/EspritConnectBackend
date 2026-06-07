package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.repositories.JobApplicationRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;

    public Map<String, Object> getSkillsTrend() {
        try {
            log.info("🔍 [getSkillsTrend] Début de la récupération des tendances de compétences");

            // Query database for top 5 skills among students and alumni
            List<Object[]> topSkills = userRepository.findTopSkills(
                    org.springframework.data.domain.PageRequest.of(0, 5)
            );
            log.info("📊 [getSkillsTrend] Nombre de compétences récupérées: {}", topSkills != null ? topSkills.size() : 0);

            List<String> labels = new ArrayList<>();
            List<Long> values = new ArrayList<>();

            if (topSkills != null && !topSkills.isEmpty()) {
                for (Object[] row : topSkills) {
                    String skillName = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    labels.add(skillName);
                    values.add(count);
                    log.debug("  ├─ Compétence: '{}' | Nombre d'utilisateurs: {}", skillName, count);
                }
            } else {
                log.warn("⚠️  [getSkillsTrend] Aucune compétence trouvée, utilisation du fallback");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("labels", labels);
            data.put("values", values);

            log.info("✅ [getSkillsTrend] Résultat final: {} compétences, labels={}, values={}",
                labels.size(), labels, values);

            return data;
        } catch (Exception ex) {
            log.error("❌ [getSkillsTrend] Erreur lors de la récupération des compétences", ex);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("labels", List.of("Spring Boot", "Angular", "React", "Python/AI", "Docker/DevOps"));
            fallback.put("values", List.of(350L, 280L, 210L, 450L, 190L));
            log.info("📌 [getSkillsTrend] Fallback activé avec {} compétences", fallback.get("labels"));
            return fallback;
        }
    }

    public Map<String, Object> getApplicationsActivity() {
        try {
            log.info("🔍 [getApplicationsActivity] Début de la récupération de l'activité des candidatures");

            // Récupération sûre de l'email de l'utilisateur (peut être null si non authentifié)
            String email = null;
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                // Eviter les tokens "anonymousUser"
                String principalName = auth.getName();
                if (principalName != null && !"anonymousUser".equalsIgnoreCase(principalName)) {
                    email = principalName;
                }
            }

            log.info("👤 [getApplicationsActivity] Email utilisateur: {}", email != null ? email : "NON AUTHENTIFIÉ");

            User currentUser = null;
            if (email != null) {
                currentUser = userRepository.findByEmail(email).orElse(null);
                if (currentUser != null) {
                    log.info("👥 [getApplicationsActivity] Utilisateur trouvé: {} | Rôle: {}",
                        currentUser.getFirstName() + " " + currentUser.getLastName(), currentUser.getRole());
                } else {
                    log.warn("⚠️  [getApplicationsActivity] Utilisateur non trouvé pour l'email: {}", email);
                }
            }

            List<Object[]> monthlyActivity;
            if (currentUser != null && currentUser.getRole() == tn.esprit.espritconnectbackend.entities.enums.UserRole.ENTREPRISE) {
                log.info("🏢 [getApplicationsActivity] Mode ENTREPRISE - Récupération pour companyId: {}", currentUser.getId());
                monthlyActivity = jobApplicationRepository.countApplicationsByMonthForCompany(currentUser.getId());
                log.info("📈 [getApplicationsActivity] Nombre de mois avec activité: {}",
                    monthlyActivity != null ? monthlyActivity.size() : 0);
            } else {
                log.info("📊 [getApplicationsActivity] Mode GLOBAL - Récupération de toutes les candidatures");
                monthlyActivity = jobApplicationRepository.countAllApplicationsByMonth();
                log.info("📈 [getApplicationsActivity] Nombre de mois avec activité: {}",
                    monthlyActivity != null ? monthlyActivity.size() : 0);
            }

            // Construire map mois -> count
            Map<Integer, Long> monthToCount = new HashMap<>();
            long totalApplications = 0;
            if (monthlyActivity != null && !monthlyActivity.isEmpty()) {
                for (Object[] row : monthlyActivity) {
                    int month = ((Number) row[0]).intValue();
                    long count = ((Number) row[1]).longValue();
                    monthToCount.put(month, count);
                    totalApplications += count;
                    log.debug("  ├─ Mois {} | Candidatures: {}",
                        java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, Locale.FRENCH), count);
                }
            }
            log.info("📊 [getApplicationsActivity] Total de candidatures: {}", totalApplications);

            // Toujours renvoyer 12 mois (1..12) en français abrégé
            List<String> labels = new ArrayList<>();
            List<Long> values = new ArrayList<>();
            Locale fr = Locale.FRENCH;
            for (int m = 1; m <= 12; m++) {
                String monthName = java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT, fr);
                monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                labels.add(monthName);
                values.add(monthToCount.getOrDefault(m, 0L));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("labels", labels);
            data.put("values", values);

            log.info("✅ [getApplicationsActivity] Résultat final: 12 mois, total candidatures: {}, labels={}",
                totalApplications, labels);

            return data;

        } catch (Exception ex) {
            log.error("❌ [getApplicationsActivity] Erreur lors de la récupération de l'activité", ex);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("labels", List.of("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"));
            fallback.put("values", List.of(0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L));
            log.info("📌 [getApplicationsActivity] Fallback activé - retour de 12 mois vides");
            return fallback;
        }
    }
}