package tn.esprit.espritconnectbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.HomepageSettingsDto;
import tn.esprit.espritconnectbackend.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class SettingsController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path settingsDir = Paths.get("settings");
    private final Path homepageFile = settingsDir.resolve("homepage.json");
    private final FileStorageService fileStorageService;

    public SettingsController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/homepage")
    public ResponseEntity<HomepageSettingsDto> getHomepage() throws IOException {
        if (Files.notExists(homepageFile)) {
            return ResponseEntity.ok(new HomepageSettingsDto());
        }
        HomepageSettingsDto dto = mapper.readValue(homepageFile.toFile(), HomepageSettingsDto.class);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/homepage")
    public ResponseEntity<HomepageSettingsDto> saveHomepage(@RequestBody HomepageSettingsDto dto) throws IOException {
        if (Files.notExists(settingsDir)) {
            Files.createDirectories(settingsDir);
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(homepageFile.toFile(), dto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/homepage/banner")
    public ResponseEntity<Map<String, String>> uploadBanner(@RequestParam("file") MultipartFile file) throws IOException {
        String path = fileStorageService.saveFile(file, "banners");
        String url = "/" + path;
        return ResponseEntity.ok(Map.of("url", url));
    }

    private final Path mailingFile = settingsDir.resolve("mailing.json");

    @GetMapping("/mailing")
    public ResponseEntity<tn.esprit.espritconnectbackend.dto.MailingSettingsDto> getMailingSettings() throws IOException {
        if (Files.notExists(mailingFile)) {
            return ResponseEntity.ok(new tn.esprit.espritconnectbackend.dto.MailingSettingsDto());
        }
        tn.esprit.espritconnectbackend.dto.MailingSettingsDto dto = mapper.readValue(mailingFile.toFile(), tn.esprit.espritconnectbackend.dto.MailingSettingsDto.class);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/mailing")
    public ResponseEntity<tn.esprit.espritconnectbackend.dto.MailingSettingsDto> saveMailingSettings(@RequestBody tn.esprit.espritconnectbackend.dto.MailingSettingsDto dto) throws IOException {
        if (Files.notExists(settingsDir)) {
            Files.createDirectories(settingsDir);
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(mailingFile.toFile(), dto);
        return ResponseEntity.ok(dto);
    }
}
