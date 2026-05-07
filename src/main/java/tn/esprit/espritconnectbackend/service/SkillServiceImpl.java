package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.SkillDTO;
import tn.esprit.espritconnectbackend.entities.Skill;
import tn.esprit.espritconnectbackend.exception.ConflictException;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.SkillRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {
    private final SkillRepository skillRepository;

    @Override
    @Transactional
    public SkillDTO create(SkillDTO dto) {
        skillRepository.findByNameIgnoreCase(dto.getName())
                .ifPresent(existing -> {
                    throw new ConflictException("Cette competence existe deja");
                });
        Skill skill = Skill.builder().name(dto.getName().trim()).build();
        return toDto(skillRepository.save(skill));
    }

    @Override
    @Transactional
    public SkillDTO update(UUID id, SkillDTO dto) {
        Skill skill = findOrThrow(id);
        skillRepository.findByNameIgnoreCase(dto.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Une autre competence porte deja ce nom");
                });
        skill.setName(dto.getName().trim());
        return toDto(skillRepository.save(skill));
    }

    @Override
    public SkillDTO getById(UUID id) {
        return toDto(findOrThrow(id));
    }

    @Override
    public List<SkillDTO> getAll() {
        return skillRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Skill skill = findOrThrow(id);
        skillRepository.delete(skill);
    }

    private Skill findOrThrow(UUID id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competence introuvable avec l'id: " + id));
    }

    private SkillDTO toDto(Skill entity) {
        SkillDTO dto = new SkillDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }
}
