package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.*;
import tn.esprit.espritconnectbackend.entities.*;
import tn.esprit.espritconnectbackend.entities.enums.ConnectionStatus;
import tn.esprit.espritconnectbackend.repositories.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final EspritProfileRepository profileRepository;
    private final WorkExperienceRepository experienceRepository;
    private final OtherEducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final WillingToHelpRepository helpRepository;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final BadgeService badgeService;

    private User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @Override
    @Transactional
    public EspritProfileDTO updateEspritProfile(EspritProfileDTO dto) {
        User user = getCurrentUserEntity();
        EspritProfile profile = user.getEspritProfile();
        if (profile == null) {
            profile = new EspritProfile();
            profile.setUser(user);
        }
        profile.setFieldOfStudy(dto.getFieldOfStudy());
        profile.setDegree(dto.getDegree());
        profile.setGraduationYear(dto.getGraduationYear());
        profile.setProgram(dto.getProgram());
        profile.setInstitution(dto.getInstitution());
        
        profile = profileRepository.save(profile);
        auditService.logAction("UPDATE_ESPRIT_PROFILE", "PROFILE", profile.getId(), "Mise à jour du profil académique");
        badgeService.checkAndAwardBadges(user);
        return mapToDTO(profile);
    }

    @Override
    public EspritProfileDTO getMyEspritProfile() {
        User user = getCurrentUserEntity();
        EspritProfile profile = user.getEspritProfile();
        if (profile == null) {
            return new EspritProfileDTO();
        }
        return mapToDTO(profile);
    }

    @Override
    @Transactional
    public WorkExperienceDTO addWorkExperience(WorkExperienceDTO dto) {
        User user = getCurrentUserEntity();
        WorkExperience exp = new WorkExperience();
        exp.setUser(user);
        exp.setCompany(dto.getCompany());
        exp.setJobTitle(dto.getJobTitle());
        exp.setIndustry(dto.getIndustry());
        exp.setJobFunction(dto.getJobFunction());
        exp.setStartDate(dto.getStartDate());
        exp.setEndDate(dto.getEndDate());
        exp.setIsCurrent(dto.getIsCurrent());
        exp.setDescription(dto.getDescription());
        
        exp = experienceRepository.save(exp);
        auditService.logAction("ADD_WORK_EXP", "WORK_EXP", exp.getId(), "Ajout expérience chez " + exp.getCompany());
        return mapToDTO(exp);
    }

    @Override
    @Transactional
    public WorkExperienceDTO updateWorkExperience(UUID id, WorkExperienceDTO dto) {
        WorkExperience exp = experienceRepository.findById(id).orElseThrow();
        exp.setCompany(dto.getCompany());
        exp.setJobTitle(dto.getJobTitle());
        exp.setIndustry(dto.getIndustry());
        exp.setJobFunction(dto.getJobFunction());
        exp.setStartDate(dto.getStartDate());
        exp.setEndDate(dto.getEndDate());
        exp.setIsCurrent(dto.getIsCurrent());
        exp.setDescription(dto.getDescription());
        return mapToDTO(experienceRepository.save(exp));
    }

    @Override
    @Transactional
    public void deleteWorkExperience(UUID id) {
        experienceRepository.deleteById(id);
    }

    @Override
    public List<WorkExperienceDTO> getMyWorkExperiences() {
        return experienceRepository.findByUser(getCurrentUserEntity()).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OtherEducationDTO addEducation(OtherEducationDTO dto) {
        User user = getCurrentUserEntity();
        OtherEducation edu = new OtherEducation();
        edu.setUser(user);
        edu.setInstitutionName(dto.getInstitutionName());
        edu.setDegree(dto.getDegree());
        edu.setGraduationYear(dto.getGraduationYear());
        
        return mapToDTO(educationRepository.save(edu));
    }

    @Override
    @Transactional
    public OtherEducationDTO updateEducation(UUID id, OtherEducationDTO dto) {
        OtherEducation edu = educationRepository.findById(id).orElseThrow();
        edu.setInstitutionName(dto.getInstitutionName());
        edu.setDegree(dto.getDegree());
        edu.setGraduationYear(dto.getGraduationYear());
        return mapToDTO(educationRepository.save(edu));
    }

    @Override
    @Transactional
    public void deleteEducation(UUID id) {
        educationRepository.deleteById(id);
    }

    @Override
    public List<OtherEducationDTO> getMyEducations() {
        return educationRepository.findByUser(getCurrentUserEntity()).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addSkill(String skillName) {
        User user = getCurrentUserEntity();
        Skill skill = skillRepository.findByName(skillName)
                .orElseGet(() -> {
                    Skill newSkill = new Skill();
                    newSkill.setName(skillName);
                    return skillRepository.save(newSkill);
                });
        user.getSkills().add(skill);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void removeSkill(UUID skillId) {
        User user = getCurrentUserEntity();
        user.getSkills().removeIf(s -> s.getId().equals(skillId));
        userRepository.save(user);
    }

    @Override
    public List<SkillDTO> getMySkills() {
        return getCurrentUserEntity().getSkills().stream()
                .map(s -> {
                    SkillDTO dto = new SkillDTO();
                    dto.setId(s.getId());
                    dto.setName(s.getName());
                    return dto;
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WillingToHelpDTO addWillingToHelp(WillingToHelpDTO dto) {
        User user = getCurrentUserEntity();
        WillingToHelp help = new WillingToHelp();
        help.setUser(user);
        help.setOfferHelp(dto.getOfferHelp());
        help.setSeekHelp(dto.getSeekHelp());
        help.setOfferMentor(dto.getOfferMentor());
        help.setSeekMentor(dto.getSeekMentor());
        WillingToHelp saved = helpRepository.save(help);
        badgeService.checkAndAwardBadges(user);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public WillingToHelpDTO updateWillingToHelp(UUID id, WillingToHelpDTO dto) {
        WillingToHelp help = helpRepository.findById(id).orElseThrow();
        help.setOfferHelp(dto.getOfferHelp());
        help.setSeekHelp(dto.getSeekHelp());
        help.setOfferMentor(dto.getOfferMentor());
        help.setSeekMentor(dto.getSeekMentor());
        WillingToHelp saved = helpRepository.save(help);
        badgeService.checkAndAwardBadges(saved.getUser());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteWillingToHelp(UUID id) {
        helpRepository.deleteById(id);
    }

    @Override
    public List<WillingToHelpDTO> getMyWillingToHelps() {
        return helpRepository.findByUser(getCurrentUserEntity()).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConnectionDTO sendConnectionRequest(UUID targetUserId) {
        User requester = getCurrentUserEntity();
        User addressee = userRepository.findById(targetUserId).orElseThrow();
        Connection connection = new Connection();
        connection.setRequester(requester);
        connection.setAddressee(addressee);
        connection.setStatus(ConnectionStatus.PENDING);
        return mapToDTO(connectionRepository.save(connection));
    }

    @Override
    @Transactional
    public ConnectionDTO acceptConnectionRequest(UUID connectionId) {
        Connection connection = connectionRepository.findById(connectionId).orElseThrow();
        connection.setStatus(ConnectionStatus.ACCEPTED);
        return mapToDTO(connectionRepository.save(connection));
    }

    @Override
    @Transactional
    public void rejectConnectionRequest(UUID connectionId) {
        Connection connection = connectionRepository.findById(connectionId).orElseThrow();
        connection.setStatus(ConnectionStatus.REJECTED);
        connectionRepository.save(connection);
    }

    @Override
    public List<ConnectionDTO> getMyConnections() {
        User user = getCurrentUserEntity();
        return connectionRepository.findByRequesterOrAddressee(user, user).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    // ── Public by userId ─────────────────────────────────────────────────────

    @Override
    public EspritProfileDTO getEspritProfileByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        EspritProfile profile = user.getEspritProfile();
        if (profile == null) return new EspritProfileDTO();
        return mapToDTO(profile);
    }

    @Override
    public List<WorkExperienceDTO> getWorkExperiencesByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return experienceRepository.findByUser(user).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<OtherEducationDTO> getEducationsByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return educationRepository.findByUser(user).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<SkillDTO> getSkillsByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return user.getSkills().stream()
                .map(s -> { SkillDTO dto = new SkillDTO(); dto.setId(s.getId()); dto.setName(s.getName()); return dto; })
                .collect(Collectors.toList());
    }

    @Override
    public List<WillingToHelpDTO> getWillingToHelpsByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return helpRepository.findByUser(user).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    // Mapping Helpers
    private EspritProfileDTO mapToDTO(EspritProfile p) {
        EspritProfileDTO dto = new EspritProfileDTO();
        dto.setId(p.getId());
        dto.setFieldOfStudy(p.getFieldOfStudy());
        dto.setDegree(p.getDegree());
        dto.setGraduationYear(p.getGraduationYear());
        dto.setProgram(p.getProgram());
        dto.setInstitution(p.getInstitution());
        return dto;
    }

    private WorkExperienceDTO mapToDTO(WorkExperience e) {
        WorkExperienceDTO dto = new WorkExperienceDTO();
        dto.setId(e.getId());
        dto.setCompany(e.getCompany());
        dto.setJobTitle(e.getJobTitle());
        dto.setIndustry(e.getIndustry());
        dto.setJobFunction(e.getJobFunction());
        dto.setStartDate(e.getStartDate());
        dto.setEndDate(e.getEndDate());
        dto.setIsCurrent(e.getIsCurrent());
        dto.setDescription(e.getDescription());
        return dto;
    }

    private OtherEducationDTO mapToDTO(OtherEducation e) {
        OtherEducationDTO dto = new OtherEducationDTO();
        dto.setId(e.getId());
        dto.setInstitutionName(e.getInstitutionName());
        dto.setDegree(e.getDegree());
        dto.setGraduationYear(e.getGraduationYear());
        return dto;
    }

    private WillingToHelpDTO mapToDTO(WillingToHelp h) {
        WillingToHelpDTO dto = new WillingToHelpDTO();
        dto.setId(h.getId());
        dto.setOfferHelp(h.getOfferHelp());
        dto.setSeekHelp(h.getSeekHelp());
        dto.setOfferMentor(h.getOfferMentor());
        dto.setSeekMentor(h.getSeekMentor());
        return dto;
    }

    private ConnectionDTO mapToDTO(Connection c) {
        ConnectionDTO dto = new ConnectionDTO();
        dto.setId(c.getId());
        dto.setRequesterId(c.getRequester().getId());
        dto.setAddresseeId(c.getAddressee().getId());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}
