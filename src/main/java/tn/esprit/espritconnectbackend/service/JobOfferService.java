package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.JobOfferDTO;

import java.util.List;
import java.util.UUID;

public interface JobOfferService {
    JobOfferDTO create(JobOfferDTO dto);
    JobOfferDTO update(UUID id, JobOfferDTO dto);
    JobOfferDTO getById(UUID id);
    List<JobOfferDTO> getAll();
    List<JobOfferDTO> getMine();
    void delete(UUID id);
}
