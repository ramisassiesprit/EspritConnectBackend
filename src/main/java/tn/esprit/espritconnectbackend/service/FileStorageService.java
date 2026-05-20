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

    public String savePostFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Get extension to determine subdirectory
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        String subDir;
        if (extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg") || 
            extension.equals("gif") || extension.equals("bmp") || extension.equals("webp")) {
            subDir = "postimages";
        } else if (extension.equals("pdf")) {
            subDir = "postpdf";
        } else if (extension.equals("doc") || extension.equals("docx")) {
            subDir = "postword";
        } else if (extension.equals("xls") || extension.equals("xlsx")) {
            subDir = "postexcel";
        } else if (extension.equals("ppt") || extension.equals("pptx")) {
            subDir = "postppt";
        } else if (extension.equals("txt")) {
            subDir = "posttxt";
        } else if (extension.isEmpty()) {
            subDir = "postfiles";
        } else {
            subDir = "post" + extension;
        }

        // Create directory if it doesn't exist under 'uploads'
        Path directoryPath = Paths.get("uploads", subDir);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        // Generate unique filename
        String fileName = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

        // Save file
        Path filePath = directoryPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return the relative path/URL
        // Format: uploads/subDir/fileName
        return "uploads/" + subDir + "/" + fileName;
    }
}
