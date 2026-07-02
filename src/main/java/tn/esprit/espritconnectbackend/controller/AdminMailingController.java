package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.AdminMailRequestDTO;
import tn.esprit.espritconnectbackend.service.Auth.EmailService;

@RestController
@RequestMapping("/admin/mailing")
@RequiredArgsConstructor
@Tag(name = "Admin Mailing", description = "Endpoints d'envoi de mails par l'admin")
public class AdminMailingController {

    private final EmailService emailService;

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Envoyer un mail personnalisé à une sélection d'utilisateurs")
    public ResponseEntity<Void> sendAdminMail(@Valid @RequestBody AdminMailRequestDTO mailRequest) {
        emailService.sendAdminCustomEmail(
                mailRequest.getEmails(),
                mailRequest.getSubject(),
                mailRequest.getMessage()
        );
        return ResponseEntity.ok().build();
    }
}
