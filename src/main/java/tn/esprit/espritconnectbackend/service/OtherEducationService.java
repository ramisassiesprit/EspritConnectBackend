package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.OtherEducationDTO;

import java.util.List;
import java.util.UUID;

public interface OtherEducationService {
    OtherEducationDTO create(OtherEducationDTO dto);
    OtherEducationDTO update(UUID id, OtherEducationDTO dto);
    OtherEducationDTO getById(UUID id);
    List<OtherEducationDTO> getMine();
    List<OtherEducationDTO> getByUserId(UUID userId);
    void delete(UUID id);
}
