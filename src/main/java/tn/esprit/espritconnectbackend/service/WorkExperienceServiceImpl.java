package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.WorkExperienceDTO;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.WorkExperience;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.exception.BadRequestException;
import tn.esprit.espritconnectbackend.exception.ForbiddenOperationException;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import tn.esprit.espritconnectbackend.repositories.WorkExperienceRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkExperienceServiceImpl implements WorkExperienceService {
    private final WorkExperienceRepository workExperienceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public WorkExperienceDTO create(WorkExperienceDTO dto) {
        validateDates(dto);
        User currentUser = getCurrentUser();
        WorkExperience entity = new WorkExperience();
        entity.setUser(currentUser);
        apply(dto, entity);
        return toDto(workExperienceRepository.save(entity));
    }

    @Override
    @Transactional
    public WorkExperienceDTO update(UUID id, WorkExperienceDTO dto) {
        validateDates(dto);
        WorkExperience entity = findOrThrow(id);
        ensureOwnerOrAdmin(entity.getUser().getId());
        apply(dto, entity);
        return toDto(workExperienceRepository.save(entity));
    }

    @Override
    public WorkExperienceDTO getById(UUID id) {
        WorkExperience entity = findOrThrow(id);
        ensureOwnerOrAdmin(entity.getUser().getId());
        return toDto(entity);
    }

    @Override
    public List<WorkExperienceDTO> getMine() {
        User currentUser = getCurrentUser();
        return workExperienceRepository.findByUserId(currentUser.getId()).stream().map(this::toDto).toList();
    }

    @Override
    public List<WorkExperienceDTO> getByUserId(UUID userId) {
        return workExperienceRepository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        WorkExperience entity = findOrThrow(id);
        ensureOwnerOrAdmin(entity.getUser().getId());
        workExperienceRepository.delete(entity);
    }

    private void validateDates(WorkExperienceDTO dto) {
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("La date de fin doit etre posterieure ou egale a la date de debut");
        }
    }

    private WorkExperience findOrThrow(UUID id) {
        return workExperienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience introuvable avec l'id: " + id));
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

    private void apply(WorkExperienceDTO dto, WorkExperience entity) {
        entity.setCompany(dto.getCompany());
        entity.setJobTitle(dto.getJobTitle());
        entity.setIndustry(dto.getIndustry());
        entity.setJobFunction(dto.getJobFunction());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setIsCurrent(dto.getIsCurrent());
        entity.setDescription(dto.getDescription());
    }

    private WorkExperienceDTO toDto(WorkExperience entity) {
        WorkExperienceDTO dto = new WorkExperienceDTO();
        dto.setId(entity.getId());
        dto.setCompany(entity.getCompany());
        dto.setJobTitle(entity.getJobTitle());
        dto.setIndustry(entity.getIndustry());
        dto.setJobFunction(entity.getJobFunction());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setIsCurrent(entity.getIsCurrent());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}
