package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.WorkExperienceDTO;
import tn.esprit.espritconnectbackend.service.WorkExperienceService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/work-experiences")
@RequiredArgsConstructor
@Tag(name = "Work Experiences", description = "Gestion des experiences professionnelles")
public class WorkExperienceController {
    private final WorkExperienceService workExperienceService;

    @PostMapping
    @Operation(summary = "Creer une experience professionnelle")
    public ResponseEntity<WorkExperienceDTO> create(@Valid @RequestBody WorkExperienceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workExperienceService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre a jour une experience professionnelle")
    public ResponseEntity<WorkExperienceDTO> update(@PathVariable UUID id, @Valid @RequestBody WorkExperienceDTO dto) {
        return ResponseEntity.ok(workExperienceService.update(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une experience professionnelle par id")
    public ResponseEntity<WorkExperienceDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workExperienceService.getById(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "Lister mes experiences professionnelles")
    public ResponseEntity<List<WorkExperienceDTO>> getMine() {
        return ResponseEntity.ok(workExperienceService.getMine());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lister les experiences d'un utilisateur")
    public ResponseEntity<List<WorkExperienceDTO>> getByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(workExperienceService.getByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une experience professionnelle")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workExperienceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
