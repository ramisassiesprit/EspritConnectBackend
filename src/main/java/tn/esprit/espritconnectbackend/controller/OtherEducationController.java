package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.OtherEducationDTO;
import tn.esprit.espritconnectbackend.service.OtherEducationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/other-educations")
@RequiredArgsConstructor
@Tag(name = "Other Educations", description = "Gestion des formations additionnelles")
public class OtherEducationController {
    private final OtherEducationService otherEducationService;

    @PostMapping
    @Operation(summary = "Creer une formation")
    public ResponseEntity<OtherEducationDTO> create(@Valid @RequestBody OtherEducationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(otherEducationService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre a jour une formation")
    public ResponseEntity<OtherEducationDTO> update(@PathVariable UUID id, @Valid @RequestBody OtherEducationDTO dto) {
        return ResponseEntity.ok(otherEducationService.update(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une formation par id")
    public ResponseEntity<OtherEducationDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(otherEducationService.getById(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "Lister mes formations")
    public ResponseEntity<List<OtherEducationDTO>> getMine() {
        return ResponseEntity.ok(otherEducationService.getMine());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lister les formations d'un utilisateur")
    public ResponseEntity<List<OtherEducationDTO>> getByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(otherEducationService.getByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une formation")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        otherEducationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
