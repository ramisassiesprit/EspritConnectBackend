package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.BadgeDTO;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.dto.EspritProfileDTO;
import tn.esprit.espritconnectbackend.dto.WillingToHelpDTO;
import tn.esprit.espritconnectbackend.dto.WorkExperienceDTO;
import tn.esprit.espritconnectbackend.dto.OtherEducationDTO;
import tn.esprit.espritconnectbackend.dto.SkillDTO;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final BadgeService badgeService;

    @Override
    public UserDTO getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateProfile(UserDTO userDTO) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setBio(userDTO.getBio());
        user.setCode(userDTO.getCode());
        user.setCompanyName(userDTO.getCompanyName());
        user.setJobTitle(userDTO.getJobTitle());
        user.setIndustry(userDTO.getIndustry());
        user.setJobFunction(userDTO.getJobFunction());
        user.setLinkedinUrl(userDTO.getLinkedinUrl());
        user.setGithubUrl(userDTO.getGithubUrl());
        user.setFacebookUrl(userDTO.getFacebookUrl());
        user.setAvatarUrl(userDTO.getAvatarUrl());
        user.setBannerUrl(userDTO.getBannerUrl());
        user.setNumTel(userDTO.getNumTel());
        if (userDTO.getRole() != null && userDTO.getRole() != UserRole.ADMIN) {
            user.setRole(userDTO.getRole());
        }
        
        User savedUser = userRepository.save(user);
        auditService.logAction("UPDATE_PROFILE", "USER", savedUser.getId(), "Profil mis à jour");
        badgeService.checkAndAwardBadges(savedUser);
        return mapToDTO(savedUser);
    }

    @Override
    public UserDTO getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'ID : " + userId));
        return mapToDTO(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getUsersByRole(tn.esprit.espritconnectbackend.entities.enums.UserRole role) {
        log.info("Récupération des utilisateurs avec le rôle : {}", role);
        return userRepository.findByRole(role).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateUserStatus(UUID userId, UserStatus status) {
        log.info("Mise à jour du statut de l'utilisateur {} vers {}", userId, status);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID : " + userId));

        user.setStatus(status);
        userRepository.save(user);
        auditService.logAction("UPDATE_STATUS", "USER", userId, "Statut changé en " + status);
    }
    @Override
    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
    }
    @Override
    public List<UserDTO> getOnlineUsers() {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByIsOnlineTrue().stream()
                .filter(user -> !user.getEmail().equals(currentEmail))
                .filter(user -> user.getRole().equals(UserRole.ETUDIANT)||user.getRole().equals(UserRole.ALUMNI)||user.getRole().equals(UserRole.ENSEIGNANT))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getDirectoryUsers() {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> !user.getEmail().equals(currentEmail))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBannerUrl(user.getBannerUrl());
        dto.setBio(user.getBio());
        dto.setCode(user.getCode());
        dto.setCompanyName(user.getCompanyName());
        dto.setJobTitle(user.getJobTitle());
        dto.setIndustry(user.getIndustry());
        dto.setJobFunction(user.getJobFunction());
        dto.setLinkedinUrl(user.getLinkedinUrl());
        dto.setGithubUrl(user.getGithubUrl());
        dto.setFacebookUrl(user.getFacebookUrl());
        dto.setIsMentor(user.getIsMentor());
        dto.setMentorAvailable(user.getMentorAvailable());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setNumTel(user.getNumTel());
        dto.setIsOnline(user.getIsOnline());
        if (user.getUserBadges() != null) {
            dto.setBadges(user.getUserBadges().stream()
                .map(ub -> BadgeDTO.builder()
                    .id(ub.getBadge().getId())
                    .name(ub.getBadge().getName())
                    .description(ub.getBadge().getDescription())
                    .iconUrl(ub.getBadge().getIconUrl())
                    .type(ub.getBadge().getType())
                    .earnedAt(ub.getEarnedAt())
                    .build())
                .collect(java.util.stream.Collectors.toList()));
        }
        if (user.getEspritProfile() != null) {
            EspritProfileDTO epDto = new EspritProfileDTO();
            epDto.setId(user.getEspritProfile().getId());
            epDto.setFieldOfStudy(user.getEspritProfile().getFieldOfStudy());
            epDto.setDegree(user.getEspritProfile().getDegree());
            epDto.setGraduationYear(user.getEspritProfile().getGraduationYear());
            epDto.setProgram(user.getEspritProfile().getProgram());
            epDto.setInstitution(user.getEspritProfile().getInstitution());
            dto.setEspritProfile(epDto);
        }
        if (user.getWillingToHelps() != null) {
            dto.setWillingToHelps(user.getWillingToHelps().stream()
                .map(w -> {
                    WillingToHelpDTO wDto = new WillingToHelpDTO();
                    wDto.setId(w.getId());
                    wDto.setOfferHelp(w.getOfferHelp());
                    wDto.setSeekHelp(w.getSeekHelp());
                    wDto.setOfferMentor(w.getOfferMentor());
                    wDto.setSeekMentor(w.getSeekMentor());
                    return wDto;
                })
                .collect(java.util.stream.Collectors.toList()));
        }
        if (user.getWorkExperiences() != null) {
            dto.setWorkExperiences(user.getWorkExperiences().stream()
                .map(w -> {
                    WorkExperienceDTO wDto = new WorkExperienceDTO();
                    wDto.setId(w.getId());
                    wDto.setCompany(w.getCompany());
                    wDto.setJobTitle(w.getJobTitle());
                    wDto.setIndustry(w.getIndustry());
                    wDto.setJobFunction(w.getJobFunction());
                    wDto.setStartDate(w.getStartDate());
                    wDto.setEndDate(w.getEndDate());
                    wDto.setIsCurrent(w.getIsCurrent());
                    wDto.setDescription(w.getDescription());
                    return wDto;
                })
                .collect(java.util.stream.Collectors.toList()));
        }
        if (user.getOtherEducations() != null) {
            dto.setOtherEducations(user.getOtherEducations().stream()
                .map(oe -> {
                    OtherEducationDTO oeDto = new OtherEducationDTO();
                    oeDto.setId(oe.getId());
                    oeDto.setInstitutionName(oe.getInstitutionName());
                    oeDto.setDegree(oe.getDegree());
                    oeDto.setGraduationYear(oe.getGraduationYear());
                    return oeDto;
                })
                .collect(java.util.stream.Collectors.toList()));
        }
        if (user.getSkills() != null) {
            dto.setSkills(user.getSkills().stream()
                .map(s -> {
                    SkillDTO sDto = new SkillDTO();
                    sDto.setId(s.getId());
                    sDto.setName(s.getName());
                    return sDto;
                })
                .collect(java.util.stream.Collectors.toList()));
        }
        return dto;
    }
}
