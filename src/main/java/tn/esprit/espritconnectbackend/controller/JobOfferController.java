package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.JobOfferDTO;
import tn.esprit.espritconnectbackend.service.JobOfferService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/job-offers")
@RequiredArgsConstructor
@Tag(name = "Job Offers", description = "Gestion des offres d'emploi")
public class JobOfferController {
    private final JobOfferService jobOfferService;

    @PostMapping
    @PreAuthorize("hasRole('ENTREPRISE')")
    @Operation(summary = "Creer une offre d'emploi")
    public ResponseEntity<JobOfferDTO> create(@Valid @RequestBody JobOfferDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobOfferService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ENTREPRISE')")
    @Operation(summary = "Mettre a jour une offre d'emploi")
    public ResponseEntity<JobOfferDTO> update(@PathVariable UUID id, @Valid @RequestBody JobOfferDTO dto) {
        return ResponseEntity.ok(jobOfferService.update(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une offre d'emploi par id")
    public ResponseEntity<JobOfferDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobOfferService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les offres")
    public ResponseEntity<List<JobOfferDTO>> getAll() {
        return ResponseEntity.ok(jobOfferService.getAll());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les offres en attente (Admin)")
    public ResponseEntity<List<JobOfferDTO>> getPending() {
        return ResponseEntity.ok(jobOfferService.getPending());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('ENTREPRISE')")
    @Operation(summary = "Lister mes offres")
    public ResponseEntity<List<JobOfferDTO>> getMine() {
        return ResponseEntity.ok(jobOfferService.getMine());
    }

    @GetMapping("/target-fields")
    @PreAuthorize("hasRole('ENTREPRISE')")
    @Operation(summary = "Lister les champs d'etude disponibles depuis les profils etudiants")
    public ResponseEntity<List<String>> getTargetFieldOptions() {
        return ResponseEntity.ok(jobOfferService.getTargetFieldOptions());
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approuver une offre (Admin)")
    public ResponseEntity<JobOfferDTO> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(jobOfferService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejeter une offre (Admin)")
    public ResponseEntity<JobOfferDTO> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(jobOfferService.reject(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENTREPRISE','ADMIN')")
    @Operation(summary = "Supprimer une offre")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        jobOfferService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ENTREPRISE')")
    @Operation(summary = "Uploader une image pour une offre")
    public ResponseEntity<JobOfferDTO> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(jobOfferService.uploadImage(id, file));
    }
}
