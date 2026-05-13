package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.*;
import tn.esprit.espritconnectbackend.service.ProfileService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints pour gérer les détails du profil utilisateur")
public class ProfileController {

    private final ProfileService profileService;

    // --- Esprit Profile ---
    @PutMapping("/esprit")
    @Operation(summary = "Mettre à jour le profil académique Esprit")
    public ResponseEntity<EspritProfileDTO> updateEspritProfile(@Valid @RequestBody EspritProfileDTO dto) {
        return ResponseEntity.ok(profileService.updateEspritProfile(dto));
    }

    @GetMapping("/esprit")
    @Operation(summary = "Récupérer mon profil académique Esprit")
    public ResponseEntity<EspritProfileDTO> getMyEspritProfile() {
        return ResponseEntity.ok(profileService.getMyEspritProfile());
    }

    // --- Work Experience ---
    @PostMapping("/experience")
    public ResponseEntity<WorkExperienceDTO> addExperience(@Valid @RequestBody WorkExperienceDTO dto) {
        return ResponseEntity.ok(profileService.addWorkExperience(dto));
    }

    @GetMapping("/experience")
    public ResponseEntity<List<WorkExperienceDTO>> getMyExperiences() {
        return ResponseEntity.ok(profileService.getMyWorkExperiences());
    }

    @DeleteMapping("/experience/{id}")
    public ResponseEntity<Void> deleteExperience(@PathVariable UUID id) {
        profileService.deleteWorkExperience(id);
        return ResponseEntity.noContent().build();
    }

    // --- Education ---
    @PostMapping("/education")
    public ResponseEntity<OtherEducationDTO> addEducation(@Valid @RequestBody OtherEducationDTO dto) {
        return ResponseEntity.ok(profileService.addEducation(dto));
    }

    @GetMapping("/education")
    public ResponseEntity<List<OtherEducationDTO>> getMyEducations() {
        return ResponseEntity.ok(profileService.getMyEducations());
    }

    // --- Skills ---
    @PostMapping("/skills")
    public ResponseEntity<Void> addSkill(@RequestParam String name) {
        profileService.addSkill(name);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/skills")
    public ResponseEntity<List<SkillDTO>> getMySkills() {
        return ResponseEntity.ok(profileService.getMySkills());
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable UUID id) {
        profileService.removeSkill(id);
        return ResponseEntity.noContent().build();
    }

    // --- Connections ---
    @PostMapping("/connections/request/{userId}")
    public ResponseEntity<ConnectionDTO> sendRequest(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.sendConnectionRequest(userId));
    }

    @PostMapping("/connections/accept/{connectionId}")
    public ResponseEntity<ConnectionDTO> acceptRequest(@PathVariable UUID connectionId) {
        return ResponseEntity.ok(profileService.acceptConnectionRequest(connectionId));
    }

    // --- Willing to Help ---
    @PostMapping("/help")
    public ResponseEntity<WillingToHelpDTO> addHelp(@Valid @RequestBody WillingToHelpDTO dto) {
        return ResponseEntity.ok(profileService.addWillingToHelp(dto));
    }

    @PutMapping("/help/{id}")
    public ResponseEntity<WillingToHelpDTO> updateHelp(@PathVariable UUID id, @Valid @RequestBody WillingToHelpDTO dto) {
        return ResponseEntity.ok(profileService.updateWillingToHelp(id, dto));
    }

    @GetMapping("/help")
    public ResponseEntity<List<WillingToHelpDTO>> getMyHelps() {
        return ResponseEntity.ok(profileService.getMyWillingToHelps());
    }

    @DeleteMapping("/help/{id}")
    public ResponseEntity<Void> deleteHelp(@PathVariable UUID id) {
        profileService.deleteWillingToHelp(id);
        return ResponseEntity.noContent().build();
    }
}
