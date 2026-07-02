package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.entities.EmailHistory;
import tn.esprit.espritconnectbackend.service.Auth.EmailService;
import tn.esprit.espritconnectbackend.service.FileStorageService;

import java.io.IOException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
@Tag(name = "Admin Email", description = "Admin email management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class EmailAdminController {

    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    @PostMapping("/send")
    @Operation(summary = "Send a custom email to a recipient")
    public ResponseEntity<EmailHistory> sendCustomEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false, defaultValue = "Admin") String sentBy) throws IOException {
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.savePostFile(image);
        }
        EmailHistory result = emailService.sendCustomEmail(to, subject, message, imageUrl, sentBy);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    @Operation(summary = "Get email sending history")
    public ResponseEntity<List<EmailHistory>> getEmailHistory() {
        return ResponseEntity.ok(emailService.getEmailHistory());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get email statistics")
    public ResponseEntity<Map<String, Object>> getEmailStats() {
        return ResponseEntity.ok(emailService.getEmailStats());
    }

    @GetMapping("/settings/birthday")
    @Operation(summary = "Get birthday email settings")
    public ResponseEntity<Map<String, Object>> getBirthdaySettings() {
        Map<String, Object> settings = Map.of(
                "enabled", emailService.isBirthdayEmailEnabled(),
                "template", emailService.getBirthdayEmailTemplate()
        );
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings/birthday")
    @Operation(summary = "Update birthday email settings")
    public ResponseEntity<Map<String, Object>> updateBirthdaySettings(@RequestBody Map<String, Object> body) {
        if (body.containsKey("enabled")) {
            emailService.setBirthdayEmailEnabled(Boolean.parseBoolean(body.get("enabled").toString()));
        }
        if (body.containsKey("template")) {
            emailService.setBirthdayEmailTemplate(body.get("template").toString());
        }
        return ResponseEntity.ok(Map.of(
                "enabled", emailService.isBirthdayEmailEnabled(),
                "template", emailService.getBirthdayEmailTemplate()
        ));
    }

    @GetMapping("/birthdays/upcoming")
    @Operation(summary = "Get upcoming birthdays")
    public ResponseEntity<List<Map<String, Object>>> getUpcomingBirthdays() {
        return ResponseEntity.ok(emailService.getUpcomingBirthdays());
    }

    @PostMapping("/birthdays/send-today")
    @Operation(summary = "Send birthday emails for today's birthdays")
    public ResponseEntity<Map<String, Object>> sendBirthdayEmailsToday() {
        int sent = emailService.sendBirthdayEmailsForToday();
        return ResponseEntity.ok(Map.of("sent", sent));
    }
}
