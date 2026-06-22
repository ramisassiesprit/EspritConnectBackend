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
public class InterviewAiService {

    @Value("${nvidia.api.key}")
    private String apiKey;

    private static final String INVOKE_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL = "mistralai/ministral-14b-instruct-2512";
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    public Map<String, Object> generateQuestions(String jobDescription) {
        String prompt = "Tu es un recruteur technique expert. Basé sur cette description de poste : '"
                + jobDescription
                + "', génère 3 questions d'entretien pertinentes (2 questions techniques et 1 question comportementale)."
                + " Retourne UNIQUEMENT un JSON valide, sans aucun texte avant ou après, au format exact : "
                + "{\"questions\": [\"question 1\", \"question 2\", \"question 3\"]}";
        return callNvidia(prompt);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> evaluateAnswers(Map<String, Object> payload) {
        try {
            // The frontend sends { qna: [{question, answer}, ...] }
            Object qnaRaw = payload.get("qna");
            StringBuilder sb = new StringBuilder();
            if (qnaRaw instanceof List) {
                List<Map<String, String>> qnaList = (List<Map<String, String>>) qnaRaw;
                int i = 1;
                for (Map<String, String> item : qnaList) {
                    sb.append("Question ").append(i).append(": ").append(item.get("question")).append("\n");
                    sb.append("Réponse ").append(i).append(": ").append(item.get("answer")).append("\n\n");
                    i++;
                }
            } else {
                sb.append(mapper.writeValueAsString(payload));
            }

            String prompt = "Tu es un recruteur RH bienveillant et expert. Voici les questions et réponses d'un candidat lors d'un entretien simulé :\n\n"
                    + sb.toString()
                    + "\nÉvalue les réponses du candidat. "
                    + "Retourne UNIQUEMENT un objet JSON valide, sans aucun texte avant ou après, sans backtick, sans markdown. "
                    + "Le JSON doit OBLIGATOIREMENT avoir ces 3 clés : "
                    + "\"score\" (entier de 0 à 10), "
                    + "\"feedback\" (chaîne de 2-3 phrases de feedback détaillé sur la qualité des réponses), "
                    + "\"tips\" (chaîne de 2-3 conseils pratiques pour améliorer les performances lors d'un vrai entretien). "
                    + "Exemple : {\"score\": 7, \"feedback\": \"Le candidat a bien structuré ses réponses...\", \"tips\": \"Utilisez la méthode STAR pour structurer vos réponses...\"}";

            return callNvidia(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Erreur lors de l'évaluation: " + e.getMessage());
            return err;
        }
    }

    private Map<String, Object> callNvidia(String prompt) {
        try {
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", MODEL);
            payload.put("max_tokens", 600);
            payload.put("temperature", 0.4);
            payload.put("top_p", 0.9);
            payload.put("messages", List.of(userMsg));

            String requestBody = mapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(INVOKE_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[InterviewAiService] HTTP Status: " + response.statusCode());

            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode rootNode = mapper.readTree(response.body());
                JsonNode contentNode = rootNode.at("/choices/0/message/content");
                if (!contentNode.isMissingNode()) {
                    String jsonString = contentNode.asText().trim();
                    System.out.println("[InterviewAiService] Raw response: " + jsonString);

                    // Strip markdown fences if present
                    jsonString = jsonString.replaceAll("(?s)^```(json)?\\s*", "").replaceAll("(?s)```\\s*$", "").trim();

                    // Find the start of JSON object
                    int start = jsonString.indexOf('{');
                    if (start > 0) jsonString = jsonString.substring(start);

                    return mapper.readValue(jsonString, Map.class);
                }
            } else {
                System.err.println("[InterviewAiService] API Error: " + response.statusCode() + " | " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "L'IA n'a pas pu générer une réponse. Vérifiez votre connexion et réessayez.");
        return fallback;
    }
}
