package tn.esprit.espritconnectbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.espritconnectbackend.dto.MentoringPreferencesDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/admin/settings")
public class MentoringSettingsController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path settingsDir = Paths.get("settings");
    private final Path mentoringPrefsFile = settingsDir.resolve("mentoring-preferences.json");

    @GetMapping("/mentoring-preferences")
    public ResponseEntity<MentoringPreferencesDto> getMentoringPreferences() throws IOException {
        if (Files.notExists(mentoringPrefsFile)) {
            return ResponseEntity.ok(new MentoringPreferencesDto());
        }
        MentoringPreferencesDto dto = mapper.readValue(mentoringPrefsFile.toFile(), MentoringPreferencesDto.class);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/mentoring-preferences")
    public ResponseEntity<MentoringPreferencesDto> saveMentoringPreferences(@RequestBody MentoringPreferencesDto dto) throws IOException {
        if (Files.notExists(settingsDir)) {
            Files.createDirectories(settingsDir);
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(mentoringPrefsFile.toFile(), dto);
        return ResponseEntity.ok(dto);
    }
}
