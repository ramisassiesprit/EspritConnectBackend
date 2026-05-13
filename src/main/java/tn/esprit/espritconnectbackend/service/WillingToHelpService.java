package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.WillingToHelpDTO;
import java.util.List;
import java.util.UUID;

public interface WillingToHelpService {
    WillingToHelpDTO create(WillingToHelpDTO willingToHelpDTO);
    WillingToHelpDTO update(UUID id, WillingToHelpDTO willingToHelpDTO);
    WillingToHelpDTO getById(UUID id);
    List<WillingToHelpDTO> getByUserId(UUID userId);
    void delete(UUID id);
    List<WillingToHelpDTO> getAll();
}

