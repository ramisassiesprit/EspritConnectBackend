package tn.esprit.espritconnectbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import tn.esprit.espritconnectbackend.dto.MailingSettingsDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class MailingSettingsService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path mailingFile = Paths.get("settings", "mailing.json");

    public MailingSettingsDto getSettings() {
        if (Files.notExists(mailingFile)) {
            return new MailingSettingsDto();
        }
        try {
            return mapper.readValue(mailingFile.toFile(), MailingSettingsDto.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new MailingSettingsDto();
        }
    }
}
