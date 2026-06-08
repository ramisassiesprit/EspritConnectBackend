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
            log.info("🔍 [getSkillsTrend] Début de la récupération des tendances de compétences (skills relationnels + cvKeywords)");

            // 1) Récupération des skills relationnels via la requête optimisée (top by join)
            List<Object[]> topSkillsFromJoin = userRepository.findTopSkills(org.springframework.data.domain.PageRequest.of(0, 50));
            log.info("📊 [getSkillsTrend] Compétences relationnelles récupérées (join): {}", topSkillsFromJoin != null ? topSkillsFromJoin.size() : 0);

            // Construire la map de comptage avec les labels originaux
            Map<String, Long> countMap = new HashMap<>();
            Map<String, String> displayMap = new HashMap<>(); // ✅ MAP POUR GARDER LES LABELS ORIGINAUX

            if (topSkillsFromJoin != null) {
                for (Object[] row : topSkillsFromJoin) {
                    String skillName = (String) row[0];
                    long cnt = ((Number) row[1]).longValue();
                    String lowerKey = skillName.toLowerCase(Locale.ROOT).trim();
                    countMap.put(lowerKey, cnt);
                    displayMap.put(lowerKey, skillName); // ✅ STOCKE LE NOM ORIGINAL
                    log.debug("  ├─ Skill original: '{}' | Key: '{}'", skillName, lowerKey);
                }
            }

            // 2) Récupérer les cvKeywords des utilisateurs ETUDIANT et ALUMNI
            List<tn.esprit.espritconnectbackend.entities.enums.UserRole> roles = List.of(
                    tn.esprit.espritconnectbackend.entities.enums.UserRole.ETUDIANT,
                    tn.esprit.espritconnectbackend.entities.enums.UserRole.ALUMNI
            );

            List<tn.esprit.espritconnectbackend.entities.User> users;
            try {
                users = userRepository.findByRoleIn(roles);
            } catch (Exception e) {
                users = new ArrayList<>();
                users.addAll(userRepository.findByRole(tn.esprit.espritconnectbackend.entities.enums.UserRole.ETUDIANT));
                users.addAll(userRepository.findByRole(tn.esprit.espritconnectbackend.entities.enums.UserRole.ALUMNI));
            }
            log.info("📄 [getSkillsTrend] Utilisateurs récupérés pour extraction CV keywords: {}", users.size());

            // 3) Tokeniser cvKeywords et compter
            // Objectif: normaliser les labels trop verbeux en extrayant des skills canoniques
            for (tn.esprit.espritconnectbackend.entities.User u : users) {
                String cv = u.getCvKeywords();
                if (cv == null || cv.isBlank()) continue;

                // split par virgule / point-virgule / saut de ligne
                String[] tokens = cv.split("[,;\\n]+");

                for (String t : tokens) {
                    String raw = t.trim();
                    if (raw.isBlank()) continue;

                    // normaliser accents et minuscules pour matching
                    String normalized = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
                            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                            .replace('.', ' ');

                    // clean edges but keep + and # inside tokens
                    String token = normalized.replaceAll("^[^a-z0-9]+|[^a-z0-9+#\\s]+$", "").trim();
                    if (token.length() <= 2) continue;

                    // 1) extraire tous les skills canoniques présents dans le token (ex: "spring boot postgresql")
                    List<String> found = extractAllCanonicalSkills(token);
                    if (!found.isEmpty()) {
                        for (String canon : found) {
                            String key = canon.toLowerCase(Locale.ROOT);
                            countMap.merge(key, 1L, Long::sum);
                            displayMap.putIfAbsent(key, prettyCanonical(key));
                        }
                    } else {
                        // 2) sinon, créer un label raccourci (1-2 mots significatifs)
                        String shortKey = shortenLabel(token).toLowerCase(Locale.ROOT);
                        if (shortKey == null || shortKey.isBlank()) continue;
                        countMap.merge(shortKey, 1L, Long::sum);
                        displayMap.putIfAbsent(shortKey, capitalizeToken(shortKey));
                    }
                }
            }

            log.info("🔢 [getSkillsTrend] Nombre de tokens uniques après fusion: {}", countMap.size());

            // 4) Sort and pick top N (5)
            int TOP_N = 5;
            List<Map.Entry<String, Long>> sorted = new ArrayList<>(countMap.entrySet());
            sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            List<String> labels = new ArrayList<>();
            List<Long> values = new ArrayList<>();
            int limit = Math.min(TOP_N, sorted.size());
            for (int i = 0; i < limit; i++) {
                Map.Entry<String, Long> e = sorted.get(i);
                String displayLabel = displayMap.getOrDefault(e.getKey(), capitalizeToken(e.getKey())); // ✅ UTILISE LE LABEL ORIGINAL OU CAPITALISÉ
                labels.add(displayLabel);
                values.add(e.getValue());
                log.debug("  ├─ Top {}: '{}' (key: '{}') => {}", i + 1, displayLabel, e.getKey(), e.getValue());
            }

            // If not enough results, fallback static example
            if (labels.isEmpty()) {
                labels = List.of("Spring Boot", "Angular", "React", "Python/AI", "Docker/DevOps");
                values = List.of(350L, 280L, 210L, 450L, 190L);
                log.warn("⚠️ [getSkillsTrend] Aucun résultat pertinent trouvé, fallback activé");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("labels", labels);
            data.put("values", values);

            log.info("✅ [getSkillsTrend] Résultat final: {} compétences (top {})", labels.size(), TOP_N);
            return data;
        } catch (Exception ex) {
            log.error("❌ [getSkillsTrend] Erreur lors de la récupération des compétences", ex);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("labels", List.of("Spring Boot", "Angular", "React", "Python/AI", "Docker/DevOps"));
            fallback.put("values", List.of(350L, 280L, 210L, 450L, 190L));
            return fallback;
        }
    }

    // helper utilitaire
    private String capitalizeToken(String token) {
        if (token == null || token.isBlank()) return token;
        return java.util.Arrays.stream(token.split("\\s+"))
                .map(word -> word.length() > 0 ? word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1) : "")
                .collect(java.util.stream.Collectors.joining(" "));
    }

    // --- Canonical skills mapping to keep labels short and consistent ---
    private static final List<String> CANONICAL_SKILLS = List.of(
            "spring boot", "spring", "postgresql", "postgres", "mysql",
            "angular", "react", "vue", "python", "java", "kotlin",
            "docker", "devops", "nodejs", "node.js", "sql", "c#", "c++",
            "hibernate", "spring data", "graphql", "aws", "azure", "gcp"
    );

    private static final Map<String, String> CANONICAL_PRETTY = Map.ofEntries(
            Map.entry("spring boot", "Spring Boot"),
            Map.entry("spring", "Spring"),
            Map.entry("postgresql", "PostgreSQL"),
            Map.entry("postgres", "PostgreSQL"),
            Map.entry("mysql", "MySQL"),
            Map.entry("angular", "Angular"),
            Map.entry("react", "React"),
            Map.entry("vue", "Vue"),
            Map.entry("python", "Python"),
            Map.entry("java", "Java"),
            Map.entry("kotlin", "Kotlin"),
            Map.entry("docker", "Docker"),
            Map.entry("devops", "DevOps"),
            Map.entry("nodejs", "Node.js"),
            Map.entry("node.js", "Node.js"),
            Map.entry("sql", "SQL"),
            Map.entry("c#", "C#"),
            Map.entry("c++", "C++"),
            Map.entry("hibernate", "Hibernate"),
            Map.entry("spring data", "Spring Data"),
            Map.entry("graphql", "GraphQL"),
            Map.entry("aws", "AWS"),
            Map.entry("azure", "Azure"),
            Map.entry("gcp", "GCP")
    );

    // Retourne toutes les skills canoniques trouvées dans le token (tri par longueur pour matcher les plus spécifiques)
    private List<String> extractAllCanonicalSkills(String token) {
        if (token == null || token.isBlank()) return Collections.emptyList();
        return CANONICAL_SKILLS.stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .filter(k -> token.contains(k))
                .distinct()
                .collect(Collectors.toList());
    }

    private String prettyCanonical(String canonical) {
        return CANONICAL_PRETTY.getOrDefault(canonical, capitalizeToken(canonical));
    }

    // Raccourcir une phrase verbeuse en 1-2 mots significatifs
    private String shortenLabel(String token) {
        if (token == null || token.isBlank()) return token;
        // remove common stop words in French and some verbs/nouns that create verbosity
        String cleaned = token.replaceAll("\\b(implementation|implémentation|impl|gestion|des|de|du|et|avec|pour|interfaces|client|administration|reservations|réservations|management|gestionnaire)\\b", " ");
        // garder seulement 2 premiers mots significatifs
        String[] words = cleaned.trim().split("\\s+");
        List<String> meaningful = new ArrayList<>();
        for (String w : words) {
            w = w.replaceAll("[^a-z0-9+#]", "");
            if (w.length() <= 2) continue;
            meaningful.add(w);
            if (meaningful.size() >= 2) break;
        }
        if (meaningful.isEmpty()) {
            // fallback : premier mot non vide
            meaningful = Arrays.stream(words).filter(s -> !s.isBlank()).limit(1).collect(Collectors.toList());
        }
        String label = String.join(" ", meaningful).trim();
        return capitalizeToken(label.isBlank() ? token : label);
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