package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.service.GeminiApiService.InterviewAiService;

import java.util.Map;

@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
@Tag(name = "Mock Interviews", description = "Simulateur d'entretiens par IA")
public class InterviewController {

    private final InterviewAiService interviewAiService;

    @PostMapping("/generate-questions")
    @Operation(summary = "Générer des questions d'entretien basées sur une description de poste")
    public ResponseEntity<Map<String, Object>> generateQuestions(@RequestBody Map<String, String> payload) {
        String jobDescription = payload.get("jobDescription");
        return ResponseEntity.ok(interviewAiService.generateQuestions(jobDescription));
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Évaluer les réponses de l'étudiant")
    public ResponseEntity<Map<String, Object>> evaluateAnswers(@RequestBody Map<String, Object> payload) {
        // payload expects "questionsAndAnswers": [{ question: "...", answer: "..." }]
        return ResponseEntity.ok(interviewAiService.evaluateAnswers(payload));
    }
}
