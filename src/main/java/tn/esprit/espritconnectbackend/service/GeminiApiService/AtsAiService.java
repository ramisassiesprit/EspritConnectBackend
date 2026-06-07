package tn.esprit.espritconnectbackend.service.GeminiApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AtsAiService {

    @Value("${nvidia.api.key}")
    private String apiKey;

    private static final String INVOKE_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL = "mistralai/ministral-14b-instruct-2512";
    private final ObjectMapper mapper = new ObjectMapper();

    public String generateCandidateSummary(String candidateProfile, String jobDescription) {
        // 🎯 Prompt très strict pour éviter les hallucinations
        String prompt = "IMPORTANT: Tu es un recruteur ATS strict. Analyse UNIQUEMENT les données fournies ci-dessous. " +
                "N'AJOUTE RIEN qui n'est pas explicitement mentionné. Ne fais pas d'hypothèses.\n\n" +
                "DONNÉES DU CANDIDAT:\n" + candidateProfile + "\n\n" +
                "EXIGENCES DU POSTE:\n" + jobDescription + "\n\n" +
                "Génère un résumé ULTRA-COMPACT en HTML:\n" +
                "<ul>\n" +
                "<li><strong>✓ Match:</strong> Liste UNIQUEMENT les compétences/formations du candidat qui correspondent au poste</li>\n" +
                "<li><strong>✗ Manque:</strong> Liste UNIQUEMENT ce qui manque au candidat pour le poste</li>\n" +
                "<li><strong>⚡ Score:</strong> Donne un score 0-100% basé UNIQUEMENT sur les données</li>\n" +
                "</ul>\n" +
                "Sois TRÈS BREF. Max 3-4 points par section. N'invente RIEN.";

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", MODEL);
            payload.put("max_tokens", 512); // 🔴 Réduit de 1024 à 512 pour limiter la taille
            payload.put("temperature", 0.3); // 🔴 Réduit de 0.7 à 0.3 pour moins d'hallucinations
            payload.put("top_p", 0.9); // 🔴 Réduit de 1.0 à 0.9 pour plus de déterminisme

            Map<String, String> userMessageMap = new HashMap<>();
            userMessageMap.put("role", "user");
            userMessageMap.put("content", prompt);

            payload.put("messages", List.of(userMessageMap));

            String requestBody = mapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(INVOKE_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode rootNode = mapper.readTree(response.body());
                JsonNode contentNode = rootNode.at("/choices/0/message/content");
                if (!contentNode.isMissingNode()) {
                    String result = contentNode.asText().trim();
                    // ✅ Nettoye les backticks si présents
                    if (result.startsWith("```")) {
                        result = result.replaceAll("```html?", "").replaceAll("```", "");
                    }
                    return result.trim();
                }
            }
            return "<ul><li><strong>Erreur:</strong> Impossible de générer le résumé</li></ul>";

        } catch (Exception e) {
            e.printStackTrace();
            return "<ul><li><strong>Erreur:</strong> " + sanitize(e.getMessage()) + "</li></ul>";
        }
    }

    // ✅ Sécurité: nettoie les caractères dangereux
    private String sanitize(String input) {
        if (input == null) return "Erreur inconnue";
        return input.replace("<", "&lt;").replace(">", "&gt;").substring(0, Math.min(100, input.length()));
    }
}