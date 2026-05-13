package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.*;
import java.util.List;
import java.util.UUID;

public interface ProfileService {
    // EspritProfile
    EspritProfileDTO updateEspritProfile(EspritProfileDTO dto);
    EspritProfileDTO getMyEspritProfile();
    
    // WorkExperience
    WorkExperienceDTO addWorkExperience(WorkExperienceDTO dto);
    WorkExperienceDTO updateWorkExperience(UUID id, WorkExperienceDTO dto);
    void deleteWorkExperience(UUID id);
    List<WorkExperienceDTO> getMyWorkExperiences();

    // OtherEducation
    OtherEducationDTO addEducation(OtherEducationDTO dto);
    OtherEducationDTO updateEducation(UUID id, OtherEducationDTO dto);
    void deleteEducation(UUID id);
    List<OtherEducationDTO> getMyEducations();

    // Skills
    void addSkill(String skillName);
    void removeSkill(UUID skillId);
    List<SkillDTO> getMySkills();

    // WillingToHelp
    WillingToHelpDTO addWillingToHelp(WillingToHelpDTO dto);
    WillingToHelpDTO updateWillingToHelp(UUID id, WillingToHelpDTO dto);
    void deleteWillingToHelp(UUID id);
    List<WillingToHelpDTO> getMyWillingToHelps();

    // Public profile by userId
    EspritProfileDTO getEspritProfileByUserId(UUID userId);
    List<WorkExperienceDTO> getWorkExperiencesByUserId(UUID userId);
    List<OtherEducationDTO> getEducationsByUserId(UUID userId);
    List<SkillDTO> getSkillsByUserId(UUID userId);
    List<WillingToHelpDTO> getWillingToHelpsByUserId(UUID userId);

    // Connections
    ConnectionDTO sendConnectionRequest(UUID targetUserId);
    ConnectionDTO acceptConnectionRequest(UUID connectionId);
    void rejectConnectionRequest(UUID connectionId);
    List<ConnectionDTO> getMyConnections();
}
