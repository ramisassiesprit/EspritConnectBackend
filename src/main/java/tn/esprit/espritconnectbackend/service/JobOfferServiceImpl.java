package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.espritconnectbackend.dto.JobOfferDTO;
import tn.esprit.espritconnectbackend.entities.JobOffer;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.JobStatus;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.exception.ForbiddenOperationException;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.EspritProfileRepository;
import tn.esprit.espritconnectbackend.repositories.JobOfferRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobOfferServiceImpl implements JobOfferService {
    private static final Path JOBS_IMAGES_DIR = Paths.get("uploads/jobsImages").toAbsolutePath().normalize();
    private static final String TARGET_FIELDS_SEPARATOR = "||";
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final EspritProfileRepository espritProfileRepository;

    @Override
    @Transactional
    public JobOfferDTO create(JobOfferDTO dto) {
        User currentUser = getCurrentUser();
        ensureCompany(currentUser);
        JobOffer jobOffer = new JobOffer();
        apply(dto, jobOffer);
        jobOffer.setCompany(resolveCompanyName(currentUser));
        jobOffer.setStatus(JobStatus.PENDING);
        jobOffer.setPublisher(currentUser);
        return toDto(jobOfferRepository.save(jobOffer));
    }

    @Override
    @Transactional
    public JobOfferDTO update(UUID id, JobOfferDTO dto) {
        User currentUser = getCurrentUser();
        ensureCompany(currentUser);
        JobOffer jobOffer = findOrThrow(id);
        ensureOwner(jobOffer.getPublisher().getId(), currentUser);
        apply(dto, jobOffer);
        jobOffer.setCompany(resolveCompanyName(currentUser));
        jobOffer.setStatus(JobStatus.PENDING);
        return toDto(jobOfferRepository.save(jobOffer));
    }

    @Override
    public JobOfferDTO getById(UUID id) {
        JobOffer entity = findOrThrow(id);
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = entity.getPublisher() != null && currentUser.getId().equals(entity.getPublisher().getId());
        if (!isAdmin && !isOwner && entity.getStatus() != JobStatus.OPEN) {
            throw new ForbiddenOperationException("Operation non autorisee");
        }
        return toDto(entity);
    }

    @Override
    public List<JobOfferDTO> getAll() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == UserRole.ADMIN) {
            return jobOfferRepository.findAll().stream().map(this::toDto).toList();
        }
        return jobOfferRepository.findByStatus(JobStatus.OPEN).stream().map(this::toDto).toList();
    }

    @Override
    public List<JobOfferDTO> getPending() {
        ensureAdmin();
        return jobOfferRepository.findByStatus(JobStatus.PENDING).stream().map(this::toDto).toList();
    }

    @Override
    public List<JobOfferDTO> getMine() {
        User currentUser = getCurrentUser();
        return jobOfferRepository.findByPublisherId(currentUser.getId()).stream().map(this::toDto).toList();
    }

    @Override
    public List<String> getTargetFieldOptions() {
        return espritProfileRepository.findDistinctFieldOfStudyValues();
    }

    @Override
    @Transactional
    public JobOfferDTO approve(UUID id) {
        ensureAdmin();
        JobOffer jobOffer = findOrThrow(id);
        jobOffer.setStatus(JobStatus.OPEN);
        return toDto(jobOfferRepository.save(jobOffer));
    }

    @Override
    @Transactional
    public JobOfferDTO reject(UUID id) {
        ensureAdmin();
        JobOffer jobOffer = findOrThrow(id);
        jobOffer.setStatus(JobStatus.REJECTED);
        return toDto(jobOfferRepository.save(jobOffer));
    }

    @Transactional
    public JobOfferDTO uploadImage(UUID id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        JobOffer jobOffer = findOrThrow(id);
        ensureOwnerOrAdmin(jobOffer.getPublisher().getId());

        try {
            Files.createDirectories(JOBS_IMAGES_DIR);
            String original = file.getOriginalFilename() == null ? "job-image" : file.getOriginalFilename();
            String safeName = sanitizeFileName(original);
            String storedName = UUID.randomUUID() + "_" + safeName;
            Path target = JOBS_IMAGES_DIR.resolve(storedName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            if (jobOffer.getImageUrl() != null && jobOffer.getImageUrl().contains("/jobImages/")) {
                String oldName = jobOffer.getImageUrl().substring(jobOffer.getImageUrl().lastIndexOf('/') + 1);
                Files.deleteIfExists(JOBS_IMAGES_DIR.resolve(oldName).normalize());
            }

            jobOffer.setImageUrl("/EspritConnect/jobImages/" + storedName);
            return toDto(jobOfferRepository.save(jobOffer));
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lors de l'upload de l'image", ex);
        }
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        JobOffer jobOffer = findOrThrow(id);
        ensureOwnerOrAdmin(jobOffer.getPublisher().getId());
        jobOfferRepository.delete(jobOffer);
    }

    private JobOffer findOrThrow(UUID id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi introuvable avec l'id: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifie introuvable: " + email));
    }

    private void ensureOwnerOrAdmin(UUID ownerId) {
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isAdmin && !currentUser.getId().equals(ownerId)) {
            throw new ForbiddenOperationException("Operation non autorisee");
        }
    }

    private void ensureOwner(UUID ownerId, User currentUser) {
        if (!currentUser.getId().equals(ownerId)) {
            throw new ForbiddenOperationException("Operation non autorisee");
        }
    }

    private void ensureCompany(User currentUser) {
        if (currentUser.getRole() != UserRole.ENTREPRISE) {
            throw new ForbiddenOperationException("Seules les entreprises peuvent creer ou modifier des offres.");
        }
    }

    private void ensureAdmin() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenOperationException("Seul un administrateur peut approuver ou rejeter des offres.");
        }
    }

    private void apply(JobOfferDTO dto, JobOffer entity) {
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setCompany(dto.getCompany());
        entity.setIndustry(dto.getIndustry());
        entity.setLocation(dto.getLocation());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setContractType(dto.getContractType());
        entity.setExperienceLevel(dto.getExperienceLevel());
        entity.setDeadline(dto.getDeadline());
        entity.setApplyUrl(dto.getApplyUrl());
        entity.setAttachmentUrl(dto.getAttachmentUrl());
        entity.setTargetFieldsOfStudy(serializeTargetFields(dto.getTargetFields()));
        if (dto.getImageUrl() != null) {
            entity.setImageUrl(dto.getImageUrl());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private JobOfferDTO toDto(JobOffer entity) {
        JobOfferDTO dto = new JobOfferDTO();
        dto.setId(entity.getId());
        dto.setPublisherId(entity.getPublisher() != null ? entity.getPublisher().getId() : null);
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setCompany(entity.getCompany());
        dto.setIndustry(entity.getIndustry());
        dto.setLocation(entity.getLocation());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setContractType(entity.getContractType());
        dto.setExperienceLevel(entity.getExperienceLevel());
        dto.setDeadline(entity.getDeadline());
        dto.setApplyUrl(entity.getApplyUrl());
        dto.setAttachmentUrl(entity.getAttachmentUrl());
        dto.setTargetFields(deserializeTargetFields(entity.getTargetFieldsOfStudy()));
        dto.setImageUrl(toPublicAssetUrl(entity.getImageUrl()));
        dto.setStatus(entity.getStatus());
        if (entity.getPublisher() != null) {
            String firstName = entity.getPublisher().getFirstName() == null ? "" : entity.getPublisher().getFirstName().trim();
            String lastName = entity.getPublisher().getLastName() == null ? "" : entity.getPublisher().getLastName().trim();
            dto.setPublisherName((firstName + " " + lastName).trim());
            dto.setPublisherAvatarUrl(entity.getPublisher().getAvatarUrl());
            dto.setPublisherJobTitle(entity.getPublisher().getJobTitle());
            dto.setPublisherCompanyName(entity.getPublisher().getCompanyName());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private String toPublicAssetUrl(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("/EspritConnect/")) {
            String pathWithoutContext = value.substring("/EspritConnect".length());
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(pathWithoutContext)
                    .toUriString();
        }
        return value;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String serializeTargetFields(List<String> input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> normalized = input.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            return null;
        }
        return String.join(TARGET_FIELDS_SEPARATOR, normalized);
    }

    private List<String> deserializeTargetFields(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return Arrays.stream(stored.split("\\Q" + TARGET_FIELDS_SEPARATOR + "\\E"))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .distinct()
                .toList();
    }

    private String resolveCompanyName(User currentUser) {
        if (currentUser.getCompanyName() != null && !currentUser.getCompanyName().trim().isEmpty()) {
            return currentUser.getCompanyName().trim();
        }
        String firstName = currentUser.getFirstName() == null ? "" : currentUser.getFirstName().trim();
        String lastName = currentUser.getLastName() == null ? "" : currentUser.getLastName().trim();
        String fallback = (firstName + " " + lastName).trim();
        return fallback.isEmpty() ? "Entreprise" : fallback;
    }
}
