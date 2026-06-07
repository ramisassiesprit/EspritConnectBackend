package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query(value = "SELECT MONTH(ja.applied_at) as month, COUNT(ja.id) FROM job_application ja " +
            "JOIN job_offer jo ON ja.job_id = jo.id " +
            "WHERE jo.publisher_id = :companyId AND YEAR(ja.applied_at) = YEAR(CURRENT_DATE) " +
            "GROUP BY MONTH(ja.applied_at) ORDER BY month", nativeQuery = true)
    List<Object[]> countApplicationsByMonthForCompany(@org.springframework.data.repository.query.Param("companyId") UUID companyId);
    @Query(value = "SELECT MONTH(ja.applied_at) as month, COUNT(ja.id) FROM job_application ja WHERE YEAR(ja.applied_at) = YEAR(CURRENT_DATE) GROUP BY MONTH(ja.applied_at) ORDER BY month", nativeQuery = true)
    List<Object[]> countAllApplicationsByMonth();
}
