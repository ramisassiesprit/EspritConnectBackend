package tn.esprit.espritconnectbackend.controller.Admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;
import tn.esprit.espritconnectbackend.service.Admin.IAdminService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Endpoints de gestion pour les administrateurs")
public class AdminController {

    private final IAdminService adminService;

    @GetMapping("/users/role/{role}")
    @Operation(summary = "Récupérer la liste des utilisateurs par rôle")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(adminService.getUsersByRole(role));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Changer le statut d'un utilisateur")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam UserStatus status
    ) {
        adminService.updateUserStatus(userId, status);
        return ResponseEntity.noContent().build();
    }
}
