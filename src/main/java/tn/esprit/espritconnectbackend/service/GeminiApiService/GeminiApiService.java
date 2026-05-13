package tn.esprit.espritconnectbackend.service.GeminiApiService;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiApiService {

    @Value("${nvidia.api.key}")
    private String apiKey;

    private static final String INVOKE_URL = "https://integrate.api.nvidia.com/v1/chat/completions";

    public String getChatCompletion(String userMessage) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Set up Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "application/json");
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Set up the Payload (Request Body)
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "google/gemma-4-31b-it");
        payload.put("max_tokens", 16384);
        payload.put("temperature", 1.00);
        payload.put("top_p", 0.95);
        // We set stream to false for basic RestTemplate handling
        payload.put("stream", false); 
        
        Map<String, Object> chatTemplateKwargs = new HashMap<>();
        chatTemplateKwargs.put("enable_thinking", true);
        payload.put("chat_template_kwargs", chatTemplateKwargs);

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", userMessage);
        payload.put("messages", List.of(message));

        // 3. Make the Request
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    INVOKE_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            return response.getBody();
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling Gemini API: " + e.getMessage();
        }
    }
}