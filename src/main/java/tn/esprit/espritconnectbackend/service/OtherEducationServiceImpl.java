package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.OtherEducationDTO;
import tn.esprit.espritconnectbackend.entities.OtherEducation;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.exception.ForbiddenOperationException;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.OtherEducationRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtherEducationServiceImpl implements OtherEducationService {
    private final OtherEducationRepository otherEducationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OtherEducationDTO create(OtherEducationDTO dto) {
        User currentUser = getCurrentUser();
        OtherEducation entity = new OtherEducation();
        entity.setUser(currentUser);
        apply(dto, entity);
        return toDto(otherEducationRepository.save(entity));
    }

    @Override
    @Transactional
    public OtherEducationDTO update(UUID id, OtherEducationDTO dto) {
        OtherEducation entity = findOrThrow(id);
        ensureOwnerOrAdmin(entity.getUser().getId());
        apply(dto, entity);
        return toDto(otherEducationRepository.save(entity));
    }

    @Override
    public OtherEducationDTO getById(UUID id) {
        OtherEducation entity = findOrThrow(id);
        ensureOwnerOrAdmin(entity.getUser().getId());
        return toDto(entity);
    }

    @Override
    public List<OtherEducationDTO> getMine() {
        User currentUser = getCurrentUser();
        return otherEducationRepository.findByUserId(currentUser.getId()).stream().map(this::toDto).toList();
    }

    @Override
    public List<OtherEducationDTO> getByUserId(UUID userId) {
        return otherEducationRepository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        OtherEducation entity = findOrThrow(id);
        ensureOwnerOrAdmin(entity.getUser().getId());
        otherEducationRepository.delete(entity);
    }

    private OtherEducation findOrThrow(UUID id) {
        return otherEducationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable avec l'id: " + id));
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

    private void apply(OtherEducationDTO dto, OtherEducation entity) {
        entity.setInstitutionName(dto.getInstitutionName());
        entity.setDegree(dto.getDegree());
        entity.setGraduationYear(dto.getGraduationYear());
    }

    private OtherEducationDTO toDto(OtherEducation entity) {
        OtherEducationDTO dto = new OtherEducationDTO();
        dto.setId(entity.getId());
        dto.setInstitutionName(entity.getInstitutionName());
        dto.setDegree(entity.getDegree());
        dto.setGraduationYear(entity.getGraduationYear());
        return dto;
    }
}
