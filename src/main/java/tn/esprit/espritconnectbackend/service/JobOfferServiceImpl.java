package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.JobOfferDTO;
import tn.esprit.espritconnectbackend.entities.JobOffer;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.exception.ForbiddenOperationException;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.JobOfferRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobOfferServiceImpl implements JobOfferService {
    private static final Path JOBS_IMAGES_DIR = Paths.get("uploads/jobsImages").toAbsolutePath().normalize();
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public JobOfferDTO create(JobOfferDTO dto) {
        User currentUser = getCurrentUser();
        JobOffer jobOffer = new JobOffer();
        apply(dto, jobOffer);
        jobOffer.setPublisher(currentUser);
        return toDto(jobOfferRepository.save(jobOffer));
    }

    @Override
    @Transactional
    public JobOfferDTO update(UUID id, JobOfferDTO dto) {
        JobOffer jobOffer = findOrThrow(id);
        ensureOwnerOrAdmin(jobOffer.getPublisher().getId());
        apply(dto, jobOffer);
        return toDto(jobOfferRepository.save(jobOffer));
    }

    @Override
    public JobOfferDTO getById(UUID id) {
        return toDto(findOrThrow(id));
    }

    @Override
    public List<JobOfferDTO> getAll() {
        return jobOfferRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public List<JobOfferDTO> getMine() {
        User currentUser = getCurrentUser();
        return jobOfferRepository.findByPublisherId(currentUser.getId()).stream().map(this::toDto).toList();
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
        entity.setImageUrl(dto.getImageUrl());
        entity.setStatus(dto.getStatus());
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
        dto.setImageUrl(entity.getImageUrl());
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

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
