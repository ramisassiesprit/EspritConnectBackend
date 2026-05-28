package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.JobApplicationDTO;
import tn.esprit.espritconnectbackend.entities.enums.ApplicationStatus;
import tn.esprit.espritconnectbackend.service.JobApplicationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/job-applications")
@RequiredArgsConstructor
@Tag(name = "Job Applications", description = "Gestion des candidatures")
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;

    @PostMapping("/upload-cv")
    @Operation(summary = "Uploader un CV pour candidature interne")
    public ResponseEntity<String> uploadCv(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationService.uploadCv(file));
    }

    @PostMapping
    @Operation(summary = "Postuler a une offre")
    public ResponseEntity<JobApplicationDTO> create(@Valid @RequestBody JobApplicationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre a jour sa candidature")
    public ResponseEntity<JobApplicationDTO> update(@PathVariable UUID id, @Valid @RequestBody JobApplicationDTO dto) {
        return ResponseEntity.ok(jobApplicationService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Mettre a jour le statut d'une candidature")
    public ResponseEntity<JobApplicationDTO> updateStatus(@PathVariable UUID id, @RequestParam ApplicationStatus status) {
        return ResponseEntity.ok(jobApplicationService.updateStatus(id, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une candidature")
    public ResponseEntity<JobApplicationDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobApplicationService.getById(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "Lister mes candidatures")
    public ResponseEntity<List<JobApplicationDTO>> getMine() {
        return ResponseEntity.ok(jobApplicationService.getMine());
    }

    @GetMapping("/by-offer/{jobOfferId}")
    @Operation(summary = "Lister les candidatures d'une offre")
    public ResponseEntity<List<JobApplicationDTO>> getByOffer(@PathVariable UUID jobOfferId) {
        return ResponseEntity.ok(jobApplicationService.getByJobOffer(jobOfferId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une candidature")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        jobApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
