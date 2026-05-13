package tn.esprit.espritconnectbackend.service.GeminiApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Service
public class GeminiApiService {

    @Value("${nvidia.api.key}")
    private String apiKey;

    private static final String INVOKE_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SseEmitter streamChatCompletion(String userMessage) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout
        
        executor.execute(() -> {
            try {
                // 1. Set up the Payload (Request Body)
                Map<String, Object> payload = new HashMap<>();
                payload.put("model", "google/gemma-2-2b-it");
                payload.put("max_tokens", 1024);
                payload.put("temperature", 0.2);
                payload.put("top_p", 0.7);
                payload.put("stream", true); // Set to stream mode!

                Map<String, String> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", userMessage);
                payload.put("messages", List.of(message));

                String requestBody = mapper.writeValueAsString(payload);

                // 2. Set up native Java HTTP client for streaming
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(INVOKE_URL))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Accept", "text/event-stream")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                // 3. Send Request and stream the response line by line
                HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());

                response.body().forEach(line -> {
                    try {
                        if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                            String json = line.substring(6); // remove "data: " prefix
                            JsonNode rootNode = mapper.readTree(json);
                            JsonNode contentNode = rootNode.at("/choices/0/delta/content");
                            if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                                // Emit just the AI's textual chunk directly to the HTTP response
                                emitter.send(contentNode.asText());
                            }
                        }
                    } catch (Exception e) {
                        emitter.completeWithError(e); // Stop streaming on parsing error
                    }
                });
                
                emitter.complete(); // Finish the stream successfully
            } catch (Exception e) {
                e.printStackTrace();
                emitter.completeWithError(e); // Halt stream entirely if call fails
            }
        });

        return emitter;
    }
}