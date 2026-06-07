package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.espritconnectbackend.dto.JobApplicationDTO;
import tn.esprit.espritconnectbackend.entities.EspritProfile;
import tn.esprit.espritconnectbackend.entities.JobApplication;
import tn.esprit.espritconnectbackend.entities.JobOffer;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.ApplicationStatus;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.exception.BadRequestException;
import tn.esprit.espritconnectbackend.exception.ConflictException;
import tn.esprit.espritconnectbackend.exception.ForbiddenOperationException;
import tn.esprit.espritconnectbackend.exception.ResourceNotFoundException;
import tn.esprit.espritconnectbackend.repositories.JobApplicationRepository;
import tn.esprit.espritconnectbackend.repositories.JobOfferRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import tn.esprit.espritconnectbackend.service.GeminiApiService.AtsAiService;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {
    private static final Path JOB_APPLICATIONS_CV_DIR = Paths.get("uploads/jobApplicationsCv").toAbsolutePath().normalize();
    private static final Pattern FILE_URL_PATTERN =
            Pattern.compile("(?i)^https?://.+\\.(pdf|doc|docx)(\\?.*)?$");
    private static final Pattern FILE_NAME_SAFE_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    @Override
    @Transactional
    public String uploadCv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier CV est obligatoire");
        }
        String original = file.getOriginalFilename() == null ? "cv.pdf" : file.getOriginalFilename();
        String extension = extractExtension(original).toLowerCase();
        if (!List.of("pdf", "doc", "docx").contains(extension)) {
            throw new BadRequestException("Format CV invalide: .pdf/.doc/.docx requis");
        }

        try {
            Files.createDirectories(JOB_APPLICATIONS_CV_DIR);
            String safeName = sanitizeFileName(original);
            String storedName = UUID.randomUUID() + "_" + safeName;
            Path target = JOB_APPLICATIONS_CV_DIR.resolve(storedName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/jobApplicationsCv/")
                    .path(storedName)
                    .toUriString();
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lors de l'upload du CV", ex);
        }
    }

    private final JobApplicationRepository jobApplicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final AtsAiService atsAiService;

    @Override
    @Transactional
    public JobApplicationDTO create(JobApplicationDTO dto) {
        User applicant = getCurrentUser();
        JobOffer jobOffer = findOfferOrThrow(dto.getJobOfferId());

        jobApplicationRepository.findByJobOfferIdAndApplicantId(jobOffer.getId(), applicant.getId())
                .ifPresent(existing -> {
                    throw new ConflictException("Vous avez deja postule a cette offre");
                });

        validateDocumentUrl(dto.getCvUrl(), "CV");
        JobApplication entity = new JobApplication();
        entity.setApplicant(applicant);
        entity.setJobOffer(jobOffer);
        entity.setCvUrl(dto.getCvUrl());
        entity.setCoverLetterUrl(dto.getCoverLetterUrl());
        entity.setStatus(ApplicationStatus.PENDING);
        
        // Generate AI Summary
        String candidateProfile = "Titre: " + applicant.getJobTitle() + "\nBio: " + applicant.getBio();
        if (applicant.getEspritProfile() != null) {
            candidateProfile += "\nDiplôme: " + applicant.getEspritProfile().getDegree() + " en " + applicant.getEspritProfile().getFieldOfStudy();
        }
        String jobDescription = "Titre: " + jobOffer.getTitle() + "\nDescription: " + jobOffer.getDescription() ;
        String summary = atsAiService.generateCandidateSummary(candidateProfile, jobDescription);
        entity.setAiSummary(summary);
        
        return toDto(jobApplicationRepository.save(entity));
    }

    @Override
    @Transactional
    public JobApplicationDTO update(UUID id, JobApplicationDTO dto) {
        JobApplication entity = findOrThrow(id);
        User currentUser = getCurrentUser();
        if (!entity.getApplicant().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("Vous ne pouvez modifier que vos candidatures");
        }

        validateDocumentUrl(dto.getCvUrl(), "CV");
        entity.setCvUrl(dto.getCvUrl());
        entity.setCoverLetterUrl(dto.getCoverLetterUrl());
        return toDto(jobApplicationRepository.save(entity));
    }

    @Override
    @Transactional
    public JobApplicationDTO updateStatus(UUID id, ApplicationStatus status) {
        JobApplication entity = findOrThrow(id);
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOfferOwner = entity.getJobOffer().getPublisher().getId().equals(currentUser.getId());
        if (!isAdmin && !isOfferOwner) {
            throw new ForbiddenOperationException("Vous ne pouvez pas modifier le statut de cette candidature");
        }
        entity.setStatus(status);
        return toDto(jobApplicationRepository.save(entity));
    }

    // Dans JobApplicationServiceImpl.java, ligne 135
@Override
@Transactional
public JobApplicationDTO regenerateSummary(UUID id) {
    JobApplication entity = findOrThrow(id);
    User applicant = entity.getApplicant();
    JobOffer jobOffer = entity.getJobOffer();

    // ✅ Réduisez les données: uniquement ce qui est pertinent pour l'ATS
    StringBuilder candidateData = new StringBuilder();

    if (applicant.getJobTitle() != null && !applicant.getJobTitle().isEmpty()) {
        candidateData.append("Titre actuel: ").append(applicant.getJobTitle()).append("\n");
    }
    if (applicant.getIndustry() != null && !applicant.getIndustry().isEmpty()) {
        candidateData.append("Secteur: ").append(applicant.getIndustry()).append("\n");
    }
    if (applicant.getCvKeywords() != null && !applicant.getCvKeywords().isEmpty()) {
        candidateData.append("Compétences: ").append(applicant.getCvKeywords()).append("\n");
    }
    if (applicant.getEspritProfile() != null) {
        EspritProfile ep = applicant.getEspritProfile();
        if (ep.getDegree() != null) {
            candidateData.append("Diplôme: ").append(ep.getDegree());
            if (ep.getFieldOfStudy() != null) {
                candidateData.append(" en ").append(ep.getFieldOfStudy());
            }
            candidateData.append("\n");
        }
        if (ep.getGraduationYear() != null) {
            candidateData.append("Année: ").append(ep.getGraduationYear()).append("\n");
        }
    }

    StringBuilder jobData = new StringBuilder();
    jobData.append("Poste: ").append(jobOffer.getTitle()).append("\n");
    if (jobOffer.getDescription() != null) {
        // Limit to first 500 chars
        String desc = jobOffer.getDescription();
        jobData.append("Description: ").append(desc.length() > 500 ? desc.substring(0, 500) + "..." : desc).append("\n");
    }
    if (jobOffer.getTargetFieldsOfStudy() != null && !jobOffer.getTargetFieldsOfStudy().isEmpty()) {
        jobData.append("Domaines demandés: ").append(jobOffer.getTargetFieldsOfStudy()).append("\n");
    }

    String summary = atsAiService.generateCandidateSummary(
        candidateData.toString(),
        jobData.toString()
    );

    entity.setAiSummary(summary);
    return toDto(jobApplicationRepository.save(entity));
}

    @Override
    public JobApplicationDTO getById(UUID id) {
        JobApplication entity = findOrThrow(id);
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = entity.getApplicant().getId().equals(currentUser.getId());
        boolean isOfferOwner = entity.getJobOffer().getPublisher().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner && !isOfferOwner) {
            throw new ForbiddenOperationException("Operation non autorisee");
        }
        return toDto(entity);
    }

    @Override
    public List<JobApplicationDTO> getMine() {
        User currentUser = getCurrentUser();
        return jobApplicationRepository.findByApplicantId(currentUser.getId()).stream().map(this::toDto).toList();
    }

    @Override
    public List<JobApplicationDTO> getByJobOffer(UUID jobOfferId) {
        User currentUser = getCurrentUser();
        JobOffer offer = findOfferOrThrow(jobOfferId);
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOfferOwner = offer.getPublisher().getId().equals(currentUser.getId());
        if (!isAdmin && !isOfferOwner) {
            throw new ForbiddenOperationException("Vous ne pouvez pas consulter ces candidatures");
        }
        return jobApplicationRepository.findByJobOfferId(jobOfferId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        JobApplication entity = findOrThrow(id);
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = entity.getApplicant().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenOperationException("Operation non autorisee");
        }
        jobApplicationRepository.delete(entity);
    }

    private void validateDocumentUrl(String url, String fieldName) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (!FILE_URL_PATTERN.matcher(url).matches()) {
            throw new BadRequestException(fieldName + " invalide: URL http(s) vers .pdf/.doc/.docx requise");
        }
    }

    private JobApplication findOrThrow(UUID id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable avec l'id: " + id));
    }

    private JobOffer findOfferOrThrow(UUID id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi introuvable avec l'id: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifie introuvable: " + email));
    }

    private JobApplicationDTO toDto(JobApplication entity) {
        JobApplicationDTO dto = new JobApplicationDTO();
        dto.setId(entity.getId());
        dto.setApplicantId(entity.getApplicant() != null ? entity.getApplicant().getId() : null);
        dto.setJobOfferId(entity.getJobOffer() != null ? entity.getJobOffer().getId() : null);
        dto.setCvUrl(entity.getCvUrl());
        dto.setCoverLetterUrl(entity.getCoverLetterUrl());
        if (entity.getApplicant() != null) {
            dto.setApplicantFirstName(entity.getApplicant().getFirstName());
            dto.setApplicantLastName(entity.getApplicant().getLastName());
            dto.setApplicantEmail(entity.getApplicant().getEmail());
            dto.setApplicantNumTel(entity.getApplicant().getNumTel());
            dto.setApplicantAvatarUrl(entity.getApplicant().getAvatarUrl());
            dto.setApplicantJobTitle(entity.getApplicant().getJobTitle());
            dto.setApplicantIndustry(entity.getApplicant().getIndustry());
            dto.setApplicantJobFunction(entity.getApplicant().getJobFunction());
            dto.setApplicantLinkedinUrl(entity.getApplicant().getLinkedinUrl());
            dto.setApplicantGithubUrl(entity.getApplicant().getGithubUrl());
            dto.setApplicantFacebookUrl(entity.getApplicant().getFacebookUrl());
            dto.setApplicantBio(entity.getApplicant().getBio());
            if (entity.getApplicant().getEspritProfile() != null) {
                dto.setApplicantGraduationYear(entity.getApplicant().getEspritProfile().getGraduationYear());
                dto.setApplicantProgram(entity.getApplicant().getEspritProfile().getProgram());
                dto.setApplicantDegree(entity.getApplicant().getEspritProfile().getDegree());
                dto.setApplicantFieldOfStudy(entity.getApplicant().getEspritProfile().getFieldOfStudy());
                dto.setApplicantInstitution(entity.getApplicant().getEspritProfile().getInstitution());
            }
        }
        dto.setStatus(entity.getStatus());
        dto.setAiSummary(entity.getAiSummary());
        dto.setAppliedAt(entity.getAppliedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1);
    }

    private String sanitizeFileName(String fileName) {
        return FILE_NAME_SAFE_PATTERN.matcher(fileName).replaceAll("_");
    }
}
