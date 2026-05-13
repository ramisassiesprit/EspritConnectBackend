package tn.esprit.espritconnectbackend.controller.GeminiApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.service.GeminiApiService.GeminiApiService;

@RestController
@RequestMapping("/api/ai")
public class GeminiApiServiceController {
     private final GeminiApiService geminiApiService;

    public GeminiApiServiceController(GeminiApiService geminiApiService) {
        this.geminiApiService = geminiApiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody String prompt) {
        String response = geminiApiService.getChatCompletion(prompt);
        return ResponseEntity.ok(response);
    }
}
