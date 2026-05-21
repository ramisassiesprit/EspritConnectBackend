package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.JobOffer;
import tn.esprit.espritconnectbackend.entities.enums.JobStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, UUID> {
    List<JobOffer> findByPublisherId(UUID publisherId);
    List<JobOffer> findByStatus(JobStatus status);
}
