package tn.esprit.espritconnectbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.CreateResourceFolderRequest;
import tn.esprit.espritconnectbackend.dto.ResourceFileDTO;
import tn.esprit.espritconnectbackend.dto.ResourceFolderDTO;
import tn.esprit.espritconnectbackend.dto.ResourceFolderDetailsDTO;
import tn.esprit.espritconnectbackend.entities.ResourceFile;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.ResourceFileRepository;
import tn.esprit.espritconnectbackend.service.ResourceService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Gestion des ressources et telechargements")
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceFileRepository resourceFileRepository;

    @GetMapping
    @Operation(summary = "Lister les dossiers de ressources")
    public ResponseEntity<List<ResourceFolderDTO>> getAllFolders() {
        return ResponseEntity.ok(resourceService.getAllFolders());
    }

    @GetMapping("/{folderId}")
    @Operation(summary = "Recuperer le detail d'un dossier")
    public ResponseEntity<ResourceFolderDetailsDTO> getFolderDetails(@PathVariable UUID folderId) {
        return ResponseEntity.ok(resourceService.getFolderDetails(folderId));
    }

    @GetMapping("/files/{fileId}/download")
    @Operation(summary = "Telecharger un fichier de ressource")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID fileId) {
        ResourceFile file = resourceFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable avec l'id: " + fileId));

        Path path = Paths.get(file.getStoragePath()).toAbsolutePath().normalize();
        FileSystemResource resource = new FileSystemResource(path);

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Fichier physique introuvable: " + path);
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.getMimeType() != null && !file.getMimeType().isBlank()) {
            mediaType = MediaType.parseMediaType(file.getMimeType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    @PostMapping("/folders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Creer un dossier de ressources (Admin)")
    public ResponseEntity<ResourceFolderDTO> createFolder(@Valid @RequestBody CreateResourceFolderRequest request) {
        return ResponseEntity.ok(resourceService.createFolder(request));
    }

    @PutMapping("/folders/{folderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre a jour un dossier de ressources (Admin)")
    public ResponseEntity<ResourceFolderDTO> updateFolder(
            @PathVariable UUID folderId,
            @Valid @RequestBody CreateResourceFolderRequest request
    ) {
        return ResponseEntity.ok(resourceService.updateFolder(folderId, request));
    }

    @PostMapping(value = "/folders/{folderId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Uploader un fichier dans un dossier (Admin)")
    public ResponseEntity<ResourceFileDTO> uploadFile(
            @PathVariable UUID folderId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(resourceService.uploadFile(folderId, file));
    }

    @DeleteMapping("/folders/{folderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un dossier (Admin)")
    public ResponseEntity<Void> deleteFolder(@PathVariable UUID folderId) {
        resourceService.deleteFolder(folderId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un fichier (Admin)")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID fileId) {
        resourceService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }
}
