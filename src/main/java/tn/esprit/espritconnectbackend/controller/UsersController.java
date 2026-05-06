package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;
import tn.esprit.espritconnectbackend.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints de gestion de users")
public class UsersController {

    private final UserService userService;

    // --- Profil Utilisateur ---
    
    @GetMapping("/me")
    @Operation(summary = "Récupérer mon propre profil")
    public ResponseEntity<UserDTO> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PutMapping("/profile")
    @Operation(summary = "Mettre à jour mon profil")
    public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateProfile(userDTO));
    }

    // --- Gestion Admin ---

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Lister tous les utilisateurs (Admin)")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/role/{role}")
    @Operation(summary = "Récupérer la liste des utilisateurs par rôle (Admin)")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/status")
    @Operation(summary = "Changer le statut d'un utilisateur (Admin)")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam UserStatus status
    ) {
        userService.updateUserStatus(userId, status);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    @Operation(summary = "Supprimer un utilisateur (Admin)")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
