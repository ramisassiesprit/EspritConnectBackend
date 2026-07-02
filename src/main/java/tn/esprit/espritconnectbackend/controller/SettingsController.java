package tn.esprit.espritconnectbackend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.HomepageSettingsDto;
import tn.esprit.espritconnectbackend.dto.JobsSettingsDto;
import tn.esprit.espritconnectbackend.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class SettingsController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path settingsDir = Paths.get("settings");
    private final Path homepageFile = settingsDir.resolve("homepage.json");
    private final Path jobsFile = settingsDir.resolve("jobs.json");
    private final FileStorageService fileStorageService;

    private static final String ETUDIANT = "ETUDIANT";
    private static final String ALUMNI = "ALUMNI";
    private static final String ENSEIGNANT = "ENSEIGNANT";
    private static final String ENTREPRISE = "ENTREPRISE";

    public SettingsController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/homepage")
    public ResponseEntity<Map<String, HomepageSettingsDto>> getHomepage() throws IOException {
        Map<String, HomepageSettingsDto> all = readAll();
        return ResponseEntity.ok(all);
    }

    @PostMapping("/homepage")
    public ResponseEntity<Map<String, HomepageSettingsDto>> saveHomepage(@RequestBody Map<String, HomepageSettingsDto> all) throws IOException {
        if (Files.notExists(settingsDir)) {
            Files.createDirectories(settingsDir);
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(homepageFile.toFile(), all);
        return ResponseEntity.ok(all);
    }

    @GetMapping("/homepage/{role}")
    public ResponseEntity<HomepageSettingsDto> getHomepageForRole(@PathVariable String role) throws IOException {
        Map<String, HomepageSettingsDto> all = readAll();
        HomepageSettingsDto dto = all.get(role.toUpperCase());
        if (dto == null) {
            dto = new HomepageSettingsDto();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/homepage/{role}")
    public ResponseEntity<HomepageSettingsDto> saveHomepageForRole(@PathVariable String role, @RequestBody HomepageSettingsDto dto) throws IOException {
        if (Files.notExists(settingsDir)) {
            Files.createDirectories(settingsDir);
        }
        Map<String, HomepageSettingsDto> all = readAll();
        all.put(role.toUpperCase(), dto);
        mapper.writerWithDefaultPrettyPrinter().writeValue(homepageFile.toFile(), all);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/homepage/{role}/banner")
    public ResponseEntity<Map<String, String>> uploadBanner(@PathVariable String role, @RequestParam("file") MultipartFile file) throws IOException {
        String folder = "banners/" + role.toLowerCase();
        String path = fileStorageService.saveFile(file, folder);
        String url = "/" + path;
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/homepage/defaults/{role}")
    public ResponseEntity<HomepageSettingsDto> getDefaultsForRole(@PathVariable String role) {
        HomepageSettingsDto defaults = new HomepageSettingsDto();
        switch (role.toUpperCase()) {
            case ENTREPRISE:
                defaults.setWebTiles(java.util.List.of("Jobs (Only)", "Social media widget"));
                defaults.setMobileTiles(java.util.List.of("Jobs (Only)", "Social media widget"));
                break;
            case ETUDIANT:
            case ALUMNI:
            case ENSEIGNANT:
            default:
                defaults.setWebTiles(java.util.List.of(
                    "Catch up + Who's online", "Recent feed posts", "Jobs (Only)",
                    "Event", "Social media widget", "Resources"
                ));
                defaults.setMobileTiles(java.util.List.of(
                    "Catch up + Who's online", "Recent feed posts", "Jobs (Only)",
                    "Event", "Social media widget", "Resources"
                ));
                break;
        }
        return ResponseEntity.ok(defaults);
    }

    private Map<String, HomepageSettingsDto> readAll() throws IOException {
        if (Files.notExists(homepageFile)) {
            return defaultMap();
        }
        try {
            Map<String, HomepageSettingsDto> map = mapper.readValue(
                homepageFile.toFile(),
                new TypeReference<Map<String, HomepageSettingsDto>>() {}
            );
            ensureRoles(map);
            return map;
        } catch (Exception e) {
            return defaultMap();
        }
    }

    private void ensureRoles(Map<String, HomepageSettingsDto> map) {
        map.putIfAbsent(ETUDIANT, new HomepageSettingsDto());
        map.putIfAbsent(ALUMNI, new HomepageSettingsDto());
        map.putIfAbsent(ENSEIGNANT, new HomepageSettingsDto());
        map.putIfAbsent(ENTREPRISE, new HomepageSettingsDto());
    }

    private Map<String, HomepageSettingsDto> defaultMap() {
        Map<String, HomepageSettingsDto> map = new HashMap<>();
        map.put(ETUDIANT, new HomepageSettingsDto());
        map.put(ALUMNI, new HomepageSettingsDto());
        map.put(ENSEIGNANT, new HomepageSettingsDto());
        map.put(ENTREPRISE, new HomepageSettingsDto());
        return map;
    }

    @GetMapping("/jobs")
    public ResponseEntity<JobsSettingsDto> getJobs() throws IOException {
        if (Files.notExists(jobsFile)) {
            return ResponseEntity.ok(new JobsSettingsDto());
        }
        JobsSettingsDto dto = mapper.readValue(jobsFile.toFile(), JobsSettingsDto.class);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/jobs")
    public ResponseEntity<JobsSettingsDto> saveJobs(@RequestBody JobsSettingsDto dto) throws IOException {
        if (Files.notExists(settingsDir)) {
            Files.createDirectories(settingsDir);
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(jobsFile.toFile(), dto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/jobs/banner")
    public ResponseEntity<Map<String, String>> uploadJobsBanner(@RequestParam("file") MultipartFile file) throws IOException {
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
