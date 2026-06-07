package tn.esprit.espritconnectbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.espritconnectbackend.entities.ResourceFile;
import tn.esprit.espritconnectbackend.entities.ResourceFolder;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;
import tn.esprit.espritconnectbackend.repositories.ResourceFolderRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ResourceBootstrapConfig {

    private final ResourceFolderRepository resourceFolderRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedResources() {
        return args -> {
            if (userRepository.findByEmail("admin@esprit.tn").isEmpty()) {
                User admin = User.builder()
                        .firstName("Admin")
                        .lastName("Esprit")
                        .email("admin@esprit.tn")
                        .passwordHash(passwordEncoder.encode("admin12345"))
                        .role(UserRole.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(admin);
                log.info("Default Admin account seeded successfully: admin@esprit.tn / admin12345");
            }

            if (resourceFolderRepository.count() > 0) {
                return;
            }

            User creator = userRepository.findAll().stream().findFirst().orElse(null);
            if (creator == null) {
                log.warn("Aucun utilisateur disponible pour initialiser les resources.");
                return;
            }

            Path resourcesDir = Paths.get("uploads/resources").toAbsolutePath().normalize();
            Files.createDirectories(resourcesDir);

            Path file1 = resourcesDir.resolve("PFE-book-Tunisie-Telecom.txt");
            Path file2 = resourcesDir.resolve("Tritux-pfe-book-23-24.txt");
            if (!Files.exists(file1)) {
                Files.writeString(file1, "Demo resource file: PFE book Tunisie Telecom");
            }
            if (!Files.exists(file2)) {
                Files.writeString(file2, "Demo resource file: Tritux pfe book 23-24");
            }

            ResourceFolder folder = ResourceFolder.builder()
                    .name("PFE BOOK 23-24")
                    .coverImageUrl("https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?q=80&w=1200&auto=format&fit=crop")
                    .creator(creator)
                    .createdAt(LocalDateTime.of(2023, 10, 3, 9, 0))
                    .updatedAt(LocalDateTime.of(2023, 11, 8, 9, 0))
                    .build();

            ResourceFile rf1 = ResourceFile.builder()
                    .folder(folder)
                    .name("PFE book-Tunisie Telecom")
                    .mimeType("text/plain")
                    .storagePath(file1.toString())
                    .createdAt(LocalDateTime.of(2023, 12, 5, 10, 0))
                    .updatedAt(LocalDateTime.of(2025, 7, 10, 9, 30))
                    .build();

            ResourceFile rf2 = ResourceFile.builder()
                    .folder(folder)
                    .name("Tritux pfe book-23-24")
                    .mimeType("text/plain")
                    .storagePath(file2.toString())
                    .createdAt(LocalDateTime.of(2023, 11, 29, 9, 0))
                    .updatedAt(LocalDateTime.of(2023, 12, 5, 11, 0))
                    .build();

            folder.getFiles().add(rf1);
            folder.getFiles().add(rf2);
            resourceFolderRepository.save(folder);

            log.info("Resource seed termine avec succes.");
        };
    }
}
