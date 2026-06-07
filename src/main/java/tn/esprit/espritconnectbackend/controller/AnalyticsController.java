package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.espritconnectbackend.service.AnalyticsService;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Données statistiques pour les tableaux de bord Entreprises")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/skills-trend")
    @Operation(summary = "Obtenir les tendances des technologies étudiées à Esprit")
    public ResponseEntity<Map<String, Object>> getSkillsTrend() {
        return ResponseEntity.ok(analyticsService.getSkillsTrend());
    }

    @GetMapping("/applications-activity")
    @Operation(summary = "Obtenir l'activité mensuelle des candidatures")
    public ResponseEntity<Map<String, Object>> getApplicationsActivity() {
        return ResponseEntity.ok(analyticsService.getApplicationsActivity());
    }
}
