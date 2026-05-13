package tn.esprit.espritconnectbackend.controller.GeminiApiService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tn.esprit.espritconnectbackend.service.GeminiApiService.GeminiApiService;

@RestController
@RequestMapping("/api/ai")
public class GeminiApiServiceController {
    
    private final GeminiApiService geminiApiService;

    public GeminiApiServiceController(GeminiApiService geminiApiService) {
        this.geminiApiService = geminiApiService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody String prompt) {
        // Return a Server-Sent Events Emitter to stream chunks in real-time
        return geminiApiService.streamChatCompletion(prompt);
    }
}
