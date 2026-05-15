package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.CreateResourceFolderRequest;
import tn.esprit.espritconnectbackend.dto.ResourceFileDTO;
import tn.esprit.espritconnectbackend.dto.ResourceFolderDTO;
import tn.esprit.espritconnectbackend.dto.ResourceFolderDetailsDTO;
import tn.esprit.espritconnectbackend.entities.ResourceFile;
import tn.esprit.espritconnectbackend.entities.ResourceFolder;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.exception.BadRequestException;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.ResourceFileRepository;
import tn.esprit.espritconnectbackend.repositories.ResourceFolderRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private static final DateTimeFormatter LABEL_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH);
    private static final Path RESOURCES_DIR = Paths.get("uploads/resources").toAbsolutePath().normalize();
    private static final Path RESOURCES_COVERS_DIR = Paths.get("uploads/resourceCovers").toAbsolutePath().normalize();

    private final ResourceFolderRepository resourceFolderRepository;
    private final ResourceFileRepository resourceFileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ResourceFolderDTO> getAllFolders() {
        return resourceFolderRepository.findAll().stream()
                .map(this::toFolderSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceFolderDetailsDTO getFolderDetails(UUID folderId) {
        ResourceFolder folder = resourceFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id: " + folderId));

        ResourceFolderDetailsDTO dto = new ResourceFolderDetailsDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setCoverImageUrl(folder.getCoverImageUrl());
        dto.setCreatorName(folder.getCreator().getFirstName() + " " + folder.getCreator().getLastName());
        dto.setCreatorAvatarUrl(folder.getCreator().getAvatarUrl());
        dto.setCreatedAtLabel(LABEL_DATE_FORMAT.format(folder.getCreatedAt()));
        dto.setUpdatedAtLabel(LABEL_DATE_FORMAT.format(folder.getUpdatedAt()));
        dto.setFiles(folder.getFiles().stream().map(this::toFileDto).toList());
        return dto;
    }

    @Override
    @Transactional
    public ResourceFolderDTO createFolder(CreateResourceFolderRequest request) {
        User creator = getCurrentUser();
        ResourceFolder folder = ResourceFolder.builder()
                .name(request.getName().trim())
                .coverImageUrl(request.getCoverImageUrl())
                .creator(creator)
                .build();
        return toFolderSummaryDto(resourceFolderRepository.save(folder));
    }

    @Override
    @Transactional
    public ResourceFolderDTO updateFolder(UUID folderId, CreateResourceFolderRequest request) {
        ResourceFolder folder = resourceFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id: " + folderId));

        folder.setName(request.getName().trim());
        folder.setCoverImageUrl(request.getCoverImageUrl());
        return toFolderSummaryDto(resourceFolderRepository.save(folder));
    }

    @Override
    @Transactional
    public ResourceFolderDTO uploadFolderCover(UUID folderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("L'image de couverture est obligatoire");
        }

        ResourceFolder folder = resourceFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id: " + folderId));

        try {
            Files.createDirectories(RESOURCES_COVERS_DIR);
            String safeOriginalName = sanitizeFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "cover-image");
            String storedName = UUID.randomUUID() + "_" + safeOriginalName;
            Path target = RESOURCES_COVERS_DIR.resolve(storedName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            if (folder.getCoverImageUrl() != null && folder.getCoverImageUrl().contains("/resourceCovers/")) {
                String oldName = folder.getCoverImageUrl().substring(folder.getCoverImageUrl().lastIndexOf('/') + 1);
                Files.deleteIfExists(RESOURCES_COVERS_DIR.resolve(oldName).normalize());
            }

            folder.setCoverImageUrl("/EspritConnect/resourceCovers/" + storedName);
            return toFolderSummaryDto(resourceFolderRepository.save(folder));
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lors de l'upload de l'image de couverture", ex);
        }
    }

    @Override
    @Transactional
    public ResourceFileDTO uploadFile(UUID folderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est obligatoire");
        }

        ResourceFolder folder = resourceFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id: " + folderId));

        try {
            Files.createDirectories(RESOURCES_DIR);
            String safeOriginalName = sanitizeFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "resource-file");
            String storedName = UUID.randomUUID() + "_" + safeOriginalName;
            Path target = RESOURCES_DIR.resolve(storedName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            ResourceFile resourceFile = ResourceFile.builder()
                    .folder(folder)
                    .name(safeOriginalName)
                    .mimeType(file.getContentType())
                    .storagePath(target.toString())
                    .build();
            return toFileDto(resourceFileRepository.save(resourceFile));
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lors de l'upload du fichier", ex);
        }
    }

    @Override
    @Transactional
    public void deleteFolder(UUID folderId) {
        ResourceFolder folder = resourceFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id: " + folderId));

        if (folder.getFiles() != null) {
            for (ResourceFile file : folder.getFiles()) {
                deletePhysicalFile(file.getStoragePath());
            }
        }
        resourceFolderRepository.delete(folder);
    }

    @Override
    @Transactional
    public void deleteFile(UUID fileId) {
        ResourceFile file = resourceFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable avec l'id: " + fileId));
        deletePhysicalFile(file.getStoragePath());
        resourceFileRepository.delete(file);
    }

    private ResourceFolderDTO toFolderSummaryDto(ResourceFolder folder) {
        ResourceFolderDTO dto = new ResourceFolderDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setCoverImageUrl(folder.getCoverImageUrl());
        dto.setCreatorName(folder.getCreator().getFirstName() + " " + folder.getCreator().getLastName());
        dto.setCreatorAvatarUrl(folder.getCreator().getAvatarUrl());
        dto.setCreatedAt(folder.getCreatedAt());
        dto.setUpdatedAt(folder.getUpdatedAt());
        dto.setItemsCount(folder.getFiles() != null ? folder.getFiles().size() : 0);
        return dto;
    }

    private ResourceFileDTO toFileDto(ResourceFile file) {
        ResourceFileDTO dto = new ResourceFileDTO();
        dto.setId(file.getId());
        dto.setName(file.getName());
        dto.setMimeType(file.getMimeType());
        dto.setCreatedAt(file.getCreatedAt());
        dto.setUpdatedAt(file.getUpdatedAt());
        dto.setDownloadUrl("/EspritConnect/resources/files/" + file.getId() + "/download");
        return dto;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifie introuvable: " + email));
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void deletePhysicalFile(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(path).toAbsolutePath().normalize());
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lors de la suppression du fichier physique: " + path, ex);
        }
    }
}
