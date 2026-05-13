package tn.esprit.espritconnectbackend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String uploadDir = "uploads/groupsImages";

    public String saveFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Create directory if it doesn't exist
        Path directoryPath = Paths.get(uploadDir, subDir);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = directoryPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return the relative path (to be stored in DB and used in URL)
        // Format: groupsImages/subDir/fileName
        return "groupsImages/" + subDir + "/" + fileName;
    }
}
