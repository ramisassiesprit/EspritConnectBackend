package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.SkillDTO;

import java.util.List;
import java.util.UUID;

public interface SkillService {
    SkillDTO create(SkillDTO dto);
    SkillDTO update(UUID id, SkillDTO dto);
    SkillDTO getById(UUID id);
    List<SkillDTO> getAll();
    void delete(UUID id);
}
