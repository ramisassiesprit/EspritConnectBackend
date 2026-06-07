package tn.esprit.espritconnectbackend.service.GeminiApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterviewAiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> generateQuestions(String jobDescription) {
        String prompt = "Agis comme un recruteur technique expert. Basé sur cette description de poste : '" + jobDescription + "', génère 3 questions d'entretien pertinentes. 2 questions techniques et 1 question comportementale. Retourne le résultat au format JSON exact suivant: {\"questions\": [\"q1\", \"q2\", \"q3\"]}. Ne retourne que le JSON.";
        return callGemini(prompt);
    }

    public Map<String, Object> evaluateAnswers(Map<String, Object> payload) {
        try {
            String qnaString = mapper.writeValueAsString(payload);
            String prompt = "Agis comme un recruteur bienveillant. Voici les questions posées et les réponses du candidat : " + qnaString + ". Évalue les réponses et donne une note sur 10, ainsi qu'un feedback constructif. Retourne le résultat au format JSON exact: {\"score\": 8, \"feedback\": \"Excellent mais...\", \"tips\": \"Parlez de la méthode STAR...\"}. Ne retourne que le JSON.";
            return callGemini(prompt);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Parsing error");
            return err;
        }
    }

    private Map<String, Object> callGemini(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();
            parts.put("text", prompt);
            Map<String, Object> contents = new HashMap<>();
            contents.put("parts", List.of(parts));
            requestBody.put("contents", List.of(contents));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL + geminiApiKey, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = mapper.readTree(response.getBody());
                JsonNode textNode = rootNode.at("/candidates/0/content/parts/0/text");
                if (!textNode.isMissingNode()) {
                    String jsonString = textNode.asText().trim();
                    if (jsonString.startsWith("```json")) jsonString = jsonString.substring(7);
                    if (jsonString.startsWith("```")) jsonString = jsonString.substring(3);
                    if (jsonString.endsWith("```")) jsonString = jsonString.substring(0, jsonString.length() - 3);
                    
                    return mapper.readValue(jsonString, Map.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "Impossible de générer via l'IA pour le moment.");
        return fallback;
    }
}
