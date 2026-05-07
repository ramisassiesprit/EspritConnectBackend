package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.JobOfferDTO;
import tn.esprit.espritconnectbackend.entities.JobOffer;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.exception.ForbiddenOperationException;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.JobOfferRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobOfferServiceImpl implements JobOfferService {
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
        entity.setContractType(dto.getContractType());
        entity.setExperienceLevel(dto.getExperienceLevel());
        entity.setDeadline(dto.getDeadline());
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
        dto.setContractType(entity.getContractType());
        dto.setExperienceLevel(entity.getExperienceLevel());
        dto.setDeadline(entity.getDeadline());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
