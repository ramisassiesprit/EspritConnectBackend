package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.JobApplicationDTO;
import tn.esprit.espritconnectbackend.entities.enums.ApplicationStatus;

import java.util.List;
import java.util.UUID;

public interface JobApplicationService {
    JobApplicationDTO create(JobApplicationDTO dto);
    JobApplicationDTO update(UUID id, JobApplicationDTO dto);
    JobApplicationDTO updateStatus(UUID id, ApplicationStatus status);
    JobApplicationDTO getById(UUID id);
    List<JobApplicationDTO> getMine();
    List<JobApplicationDTO> getByJobOffer(UUID jobOfferId);
    void delete(UUID id);
}
