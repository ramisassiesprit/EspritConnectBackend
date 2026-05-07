package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.SkillDTO;
import tn.esprit.espritconnectbackend.service.SkillService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
@Tag(name = "Skills", description = "Gestion des competences")
public class SkillController {
    private final SkillService skillService;

    @PostMapping
    @Operation(summary = "Creer une competence")
    public ResponseEntity<SkillDTO> create(@Valid @RequestBody SkillDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre a jour une competence")
    public ResponseEntity<SkillDTO> update(@PathVariable UUID id, @Valid @RequestBody SkillDTO dto) {
        return ResponseEntity.ok(skillService.update(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une competence par id")
    public ResponseEntity<SkillDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(skillService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lister les competences")
    public ResponseEntity<List<SkillDTO>> getAll() {
        return ResponseEntity.ok(skillService.getAll());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une competence")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        skillService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
