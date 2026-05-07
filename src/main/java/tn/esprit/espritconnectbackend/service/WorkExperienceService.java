package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.WorkExperienceDTO;

import java.util.List;
import java.util.UUID;

public interface WorkExperienceService {
    WorkExperienceDTO create(WorkExperienceDTO dto);
    WorkExperienceDTO update(UUID id, WorkExperienceDTO dto);
    WorkExperienceDTO getById(UUID id);
    List<WorkExperienceDTO> getMine();
    List<WorkExperienceDTO> getByUserId(UUID userId);
    void delete(UUID id);
}
