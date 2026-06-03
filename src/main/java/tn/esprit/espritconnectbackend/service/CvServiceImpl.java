package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvServiceImpl implements CvService {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public User uploadCv(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            // 1. Read bytes FIRST before any InputStream consumption
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            // 2. Save file to disk using byte array (avoids double-read of InputStream)
            String fileUrl = fileStorageService.saveCvFromBytes(fileBytes, originalFilename);
            user.setCvUrl(fileUrl);
            log.info("CV saved at {} for user {}", fileUrl, user.getEmail());

            // 3. Extract text if PDF
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
                String cvText = extractTextFromPdf(fileBytes);
                if (cvText != null) {
                    cvText = cvText.replaceAll("\\s+", " ").trim();
                    // Limit to 65000 chars to avoid TEXT column overflow
                    if (cvText.length() > 65000) {
                        cvText = cvText.substring(0, 65000);
                    }
                    user.setCvKeywords(cvText);
                    log.info("Extracted {} chars from CV for user {}", cvText.length(), user.getEmail());
                }
            }

            User savedUser = userRepository.save(user);
            userRepository.flush(); // Force immediate write to DB
            log.info("User {} saved with cvUrl={}", savedUser.getEmail(), savedUser.getCvUrl());
            return savedUser;

        } catch (IOException e) {
            log.error("Failed to process CV upload for user {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to process CV file", e);
        }
    }

    private String extractTextFromPdf(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            } else {
                log.warn("Cannot extract text from encrypted PDF.");
                return null;
            }
        } catch (IOException e) {
            log.error("Error extracting text from PDF: {}", e.getMessage());
            return null;
        }
    }
}
