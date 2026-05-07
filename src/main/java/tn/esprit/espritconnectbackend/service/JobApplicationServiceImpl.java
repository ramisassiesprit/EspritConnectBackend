package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.JobApplicationDTO;
import tn.esprit.espritconnectbackend.entities.JobApplication;
import tn.esprit.espritconnectbackend.entities.JobOffer;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.ApplicationStatus;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.exception.BadRequestException;
import tn.esprit.espritconnectbackend.exception.ConflictException;
import tn.esprit.espritconnectbackend.exception.ForbiddenOperationException;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.JobApplicationRepository;
import tn.esprit.espritconnectbackend.repositories.JobOfferRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {
    private static final Pattern FILE_URL_PATTERN =
            Pattern.compile("(?i)^https?://.+\\.(pdf|doc|docx)(\\?.*)?$");

    private final JobApplicationRepository jobApplicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public JobApplicationDTO create(JobApplicationDTO dto) {
        User applicant = getCurrentUser();
        JobOffer jobOffer = findOfferOrThrow(dto.getJobOfferId());

        jobApplicationRepository.findByJobOfferIdAndApplicantId(jobOffer.getId(), applicant.getId())
                .ifPresent(existing -> {
                    throw new ConflictException("Vous avez deja postule a cette offre");
                });

        validateDocumentUrl(dto.getCvUrl(), "CV");
        validateDocumentUrl(dto.getCoverLetterUrl(), "Lettre de motivation");

        JobApplication entity = new JobApplication();
        entity.setApplicant(applicant);
        entity.setJobOffer(jobOffer);
        entity.setCvUrl(dto.getCvUrl());
        entity.setCoverLetterUrl(dto.getCoverLetterUrl());
        entity.setStatus(ApplicationStatus.PENDING);
        return toDto(jobApplicationRepository.save(entity));
    }

    @Override
    @Transactional
    public JobApplicationDTO update(UUID id, JobApplicationDTO dto) {
        JobApplication entity = findOrThrow(id);
        User currentUser = getCurrentUser();
        if (!entity.getApplicant().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("Vous ne pouvez modifier que vos candidatures");
        }

        validateDocumentUrl(dto.getCvUrl(), "CV");
        validateDocumentUrl(dto.getCoverLetterUrl(), "Lettre de motivation");
        entity.setCvUrl(dto.getCvUrl());
        entity.setCoverLetterUrl(dto.getCoverLetterUrl());
        return toDto(jobApplicationRepository.save(entity));
    }

    @Override
    @Transactional
    public JobApplicationDTO updateStatus(UUID id, ApplicationStatus status) {
        JobApplication entity = findOrThrow(id);
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOfferOwner = entity.getJobOffer().getPublisher().getId().equals(currentUser.getId());
        if (!isAdmin && !isOfferOwner) {
            throw new ForbiddenOperationException("Vous ne pouvez pas modifier le statut de cette candidature");
        }
        entity.setStatus(status);
        return toDto(jobApplicationRepository.save(entity));
    }

    @Override
    public JobApplicationDTO getById(UUID id) {
        JobApplication entity = findOrThrow(id);
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = entity.getApplicant().getId().equals(currentUser.getId());
        boolean isOfferOwner = entity.getJobOffer().getPublisher().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner && !isOfferOwner) {
            throw new ForbiddenOperationException("Operation non autorisee");
        }
        return toDto(entity);
    }

    @Override
    public List<JobApplicationDTO> getMine() {
        User currentUser = getCurrentUser();
        return jobApplicationRepository.findByApplicantId(currentUser.getId()).stream().map(this::toDto).toList();
    }

    @Override
    public List<JobApplicationDTO> getByJobOffer(UUID jobOfferId) {
        User currentUser = getCurrentUser();
        JobOffer offer = findOfferOrThrow(jobOfferId);
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOfferOwner = offer.getPublisher().getId().equals(currentUser.getId());
        if (!isAdmin && !isOfferOwner) {
            throw new ForbiddenOperationException("Vous ne pouvez pas consulter ces candidatures");
        }
        return jobApplicationRepository.findByJobOfferId(jobOfferId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        JobApplication entity = findOrThrow(id);
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = entity.getApplicant().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenOperationException("Operation non autorisee");
        }
        jobApplicationRepository.delete(entity);
    }

    private void validateDocumentUrl(String url, String fieldName) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (!FILE_URL_PATTERN.matcher(url).matches()) {
            throw new BadRequestException(fieldName + " invalide: URL http(s) vers .pdf/.doc/.docx requise");
        }
    }

    private JobApplication findOrThrow(UUID id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable avec l'id: " + id));
    }

    private JobOffer findOfferOrThrow(UUID id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi introuvable avec l'id: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifie introuvable: " + email));
    }

    private JobApplicationDTO toDto(JobApplication entity) {
        JobApplicationDTO dto = new JobApplicationDTO();
        dto.setId(entity.getId());
        dto.setApplicantId(entity.getApplicant() != null ? entity.getApplicant().getId() : null);
        dto.setJobOfferId(entity.getJobOffer() != null ? entity.getJobOffer().getId() : null);
        dto.setCvUrl(entity.getCvUrl());
        dto.setCoverLetterUrl(entity.getCoverLetterUrl());
        dto.setStatus(entity.getStatus());
        dto.setAppliedAt(entity.getAppliedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
