package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.JobApplication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByApplicantId(UUID applicantId);
    List<JobApplication> findByJobOfferId(UUID jobOfferId);
    Optional<JobApplication> findByJobOfferIdAndApplicantId(UUID jobOfferId, UUID applicantId);
}
