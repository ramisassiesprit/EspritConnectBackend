package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.GroupDTO;
import tn.esprit.espritconnectbackend.dto.GroupMemberDTO;
import tn.esprit.espritconnectbackend.entities.Group;
import tn.esprit.espritconnectbackend.entities.GroupMember;
import tn.esprit.espritconnectbackend.entities.GroupMemberCriteria;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.GroupMemberRole;
import tn.esprit.espritconnectbackend.entities.enums.GroupPrivacy;
import tn.esprit.espritconnectbackend.entities.enums.GroupMemberStatus;
import tn.esprit.espritconnectbackend.entities.enums.GroupStatus;
import tn.esprit.espritconnectbackend.entities.enums.NotificationType;
import tn.esprit.espritconnectbackend.repositories.GroupMemberCriteriaRepository;
import tn.esprit.espritconnectbackend.repositories.GroupMemberRepository;
import tn.esprit.espritconnectbackend.repositories.GroupRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMemberCriteriaRepository groupMemberCriteriaRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    private User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @Override
    @Transactional
    public GroupDTO createGroup(GroupDTO groupDTO) {
        User creator = getCurrentUserEntity();

        Group group = new Group();
        group.setGroupName(groupDTO.getGroupName());
        group.setDescription(groupDTO.getDescription());
        group.setPrivacy(groupDTO.getPrivacy());
        group.setStatus(GroupStatus.PENDING);
        group.setCreator(creator);
        group.setWebsite(groupDTO.getWebsite());
        group.setLogoUrl(groupDTO.getLogoUrl());
        group.setBannerUrl(groupDTO.getBannerUrl());
        group.setTagging(groupDTO.getTagging() != null ? groupDTO.getTagging() : false);
        log.info("Received labels for group {}: {}", groupDTO.getGroupName(), groupDTO.getLabels());
        group.setLabels(groupDTO.getLabels());

        GroupMemberCriteria criteria = new GroupMemberCriteria();
        criteria.setGroup(group);
        criteria.setLocation(groupDTO.getLocation());
        criteria.setAffiliation(groupDTO.getAffiliation());
        criteria.setFieldOfStudy(groupDTO.getFieldOfStudy());
        criteria.setDegree(groupDTO.getDegree());
        criteria.setGraduationYear(groupDTO.getGraduationYear());
        criteria.setInstitutionProgram(groupDTO.getInstitutionProgram());
        criteria.setOtherDegree(groupDTO.getOtherDegree());
        criteria.setOtherGraduationYear(groupDTO.getOtherGraduationYear());
        criteria.setCompany(groupDTO.getCompany());
        criteria.setIndustry(groupDTO.getIndustry());
        criteria.setJobFunction(groupDTO.getJobFunction());
        criteria.setWillingOffering(groupDTO.getWillingOffering());
        criteria.setWillingSeeking(groupDTO.getWillingSeeking());
        criteria.setMentoringOffering(groupDTO.getMentoringOffering());
        criteria.setMentoringSeeking(groupDTO.getMentoringSeeking());
        group.setMemberCriteria(criteria);

        Group savedGroup = groupRepository.save(group);

        // Add creator as ADMIN member (approved)
        addMemberToGroupEntity(savedGroup, creator, GroupMemberRole.ADMIN, GroupMemberStatus.APPROVED);

        auditService.logAction("CREATE_GROUP", "GROUP", savedGroup.getId(),
                "Groupe créé : " + savedGroup.getGroupName());

        return mapToDTO(savedGroup);
    }

    @Override
    @Transactional
    public GroupDTO createGroupWithFiles(GroupDTO groupDTO, MultipartFile logoFile, MultipartFile bannerFile)
            throws java.io.IOException {
        if (logoFile != null && !logoFile.isEmpty()) {
            String logoUrl = fileStorageService.saveFile(logoFile, "icons");
            groupDTO.setLogoUrl(logoUrl);
        }
        if (bannerFile != null && !bannerFile.isEmpty()) {
            String bannerUrl = fileStorageService.saveFile(bannerFile, "banners");
            groupDTO.setBannerUrl(bannerUrl);
        }
        return createGroup(groupDTO);
    }

    @Override
    @Transactional
    public GroupDTO updateGroup(UUID groupId, GroupDTO groupDTO) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        // Check if current user is admin of the group
        User currentUser = getCurrentUserEntity();
        checkGroupAdmin(group, currentUser);

        group.setGroupName(groupDTO.getGroupName());
        group.setDescription(groupDTO.getDescription());
        group.setPrivacy(groupDTO.getPrivacy());
        group.setWebsite(groupDTO.getWebsite());
        if (groupDTO.getLogoUrl() != null && !groupDTO.getLogoUrl().trim().isEmpty()) {
            group.setLogoUrl(groupDTO.getLogoUrl());
        }
        if (groupDTO.getBannerUrl() != null && !groupDTO.getBannerUrl().trim().isEmpty()) {
            group.setBannerUrl(groupDTO.getBannerUrl());
        }
        group.setTagging(groupDTO.getTagging() != null ? groupDTO.getTagging() : group.getTagging());
        group.setLabels(groupDTO.getLabels());
        group.setUpdatedAt(java.time.LocalDateTime.now());

        // Save the group first so it has a managed ID before criteria is persisted
        Group savedGroup = groupRepository.save(group);

        // Resolve or create the member criteria using the repository to avoid
        // relying on cascade from the inverse side of the OneToOne relationship
        GroupMemberCriteria criteria = groupMemberCriteriaRepository.findByGroup(savedGroup)
                .orElseGet(() -> {
                    GroupMemberCriteria newCriteria = new GroupMemberCriteria();
                    newCriteria.setGroup(savedGroup);
                    return newCriteria;
                });

        criteria.setLocation(groupDTO.getLocation());
        criteria.setAffiliation(groupDTO.getAffiliation());
        criteria.setFieldOfStudy(groupDTO.getFieldOfStudy());
        criteria.setDegree(groupDTO.getDegree());
        criteria.setGraduationYear(groupDTO.getGraduationYear());
        criteria.setInstitutionProgram(groupDTO.getInstitutionProgram());
        criteria.setOtherDegree(groupDTO.getOtherDegree());
        criteria.setOtherGraduationYear(groupDTO.getOtherGraduationYear());
        criteria.setCompany(groupDTO.getCompany());
        criteria.setIndustry(groupDTO.getIndustry());
        criteria.setJobFunction(groupDTO.getJobFunction());
        criteria.setWillingOffering(groupDTO.getWillingOffering());
        criteria.setWillingSeeking(groupDTO.getWillingSeeking());
        criteria.setMentoringOffering(groupDTO.getMentoringOffering());
        criteria.setMentoringSeeking(groupDTO.getMentoringSeeking());

        groupMemberCriteriaRepository.save(criteria);

        // Refresh to pick up the persisted criteria in the returned DTO
        Group updatedGroup = groupRepository.findById(savedGroup.getId())
                .orElse(savedGroup);

        auditService.logAction("UPDATE_GROUP", "GROUP", updatedGroup.getId(), "Groupe mis à jour");

        return mapToDTO(updatedGroup);
    }

    @Override
    @Transactional
    public GroupDTO updateGroupWithFiles(UUID groupId, GroupDTO groupDTO, MultipartFile logoFile,
            MultipartFile bannerFile) throws java.io.IOException {
        if (logoFile != null && !logoFile.isEmpty()) {
            String logoUrl = fileStorageService.saveFile(logoFile, "icons");
            groupDTO.setLogoUrl(logoUrl);
        }
        if (bannerFile != null && !bannerFile.isEmpty()) {
            String bannerUrl = fileStorageService.saveFile(bannerFile, "banners");
            groupDTO.setBannerUrl(bannerUrl);
        }
        return updateGroup(groupId, groupDTO);
    }

    @Override
    @Transactional
    public void deleteGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        User currentUser = getCurrentUserEntity();
        checkGroupAdmin(group, currentUser);

        groupRepository.delete(group);
        auditService.logAction("DELETE_GROUP", "GROUP", groupId, "Groupe supprimé");
    }

    @Override
    public GroupDTO getGroupById(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));
        return mapToDTO(group);
    }

    @Override
    public List<GroupDTO> getAllGroups() {
        return groupRepository.findAll().stream()
                .filter(group -> group.getStatus() == GroupStatus.APPROVED)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupDTO> getPendingGroups() {
        return groupRepository.findAll().stream()
                .filter(group -> group.getStatus() == GroupStatus.PENDING)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GroupDTO approveGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        if (group.getStatus() != GroupStatus.PENDING) {
            throw new RuntimeException("Le groupe n'est pas en attente d'approbation");
        }

        group.setStatus(GroupStatus.APPROVED);
        Group savedGroup = groupRepository.save(group);

        // Notify creator
        notificationService.createNotification(
                group.getCreator(),
                "Groupe approuvé",
                "Votre groupe '" + group.getGroupName() + "' a été approuvé et est maintenant visible publiquement.",
                NotificationType.GROUP_APPROVED,
                "GROUP",
                group.getId());

        auditService.logAction("APPROVE_GROUP", "GROUP", groupId, "Groupe approuvé : " + group.getGroupName());

        return mapToDTO(savedGroup);
    }

    @Override
    @Transactional
    public GroupDTO rejectGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        if (group.getStatus() != GroupStatus.PENDING) {
            throw new RuntimeException("Le groupe n'est pas en attente d'approbation");
        }

        group.setStatus(GroupStatus.REJECTED);
        Group savedGroup = groupRepository.save(group);

        // Notify creator
        notificationService.createNotification(
                group.getCreator(),
                "Groupe rejeté",
                "Votre groupe '" + group.getGroupName() + "' a été rejeté.",
                NotificationType.GROUP_REJECTED,
                "GROUP",
                group.getId());

        auditService.logAction("REJECT_GROUP", "GROUP", groupId, "Groupe rejeté : " + group.getGroupName());

        return mapToDTO(savedGroup);
    }

    @Override
    @Transactional
    public GroupDTO setGroupStatus(UUID groupId, GroupStatus status) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        group.setStatus(status);
        Group saved = groupRepository.save(group);

        // Notify creator for important transitions
        if (status == GroupStatus.APPROVED) {
            notificationService.createNotification(
                    group.getCreator(),
                    "Groupe approuvé",
                    "Votre groupe '" + group.getGroupName() + "' a été approuvé.",
                    NotificationType.GROUP_APPROVED,
                    "GROUP",
                    group.getId());
        } else if (status == GroupStatus.REJECTED) {
            notificationService.createNotification(
                    group.getCreator(),
                    "Groupe rejeté",
                    "Votre groupe '" + group.getGroupName() + "' a été rejeté.",
                    NotificationType.GROUP_REJECTED,
                    "GROUP",
                    group.getId());
        }

        auditService.logAction("SET_GROUP_STATUS", "GROUP", groupId, "Statut changé en: " + status.name());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public GroupMemberDTO addMember(UUID groupId, UUID userId, GroupMemberRole role) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (groupMemberRepository.existsByGroupAndUser(group, userToAdd)) {
            throw new RuntimeException("L'utilisateur est déjà membre du groupe");
        }

        // Decide initial membership status based on group privacy
        GroupMemberStatus initialStatus = GroupMemberStatus.APPROVED;
        if (group.getPrivacy() != null && group.getPrivacy() != tn.esprit.espritconnectbackend.entities.enums.GroupPrivacy.PUBLIC) {
            // For PRIVATE or SECRET groups, invitations/added-by-member should be pending approval
            initialStatus = GroupMemberStatus.PENDING;
        }

        GroupMember member = addMemberToGroupEntity(group, userToAdd, role, initialStatus);

        // Notify user
        notificationService.createNotification(
                userToAdd,
                "Nouveau groupe",
                "Vous avez été ajouté au groupe : " + group.getGroupName(),
                NotificationType.GROUP_INVITE,
                "GROUP",
                group.getId());

        GroupMemberDTO memberDTO = mapToMemberDTO(member);
        broadcastGroupMembers(groupId);
        return memberDTO;
    }

        @Override
        @Transactional
        public GroupMemberDTO approveMember(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

            // Only admins/owners can approve
            User currentUser = getCurrentUserEntity();
            checkGroupAdmin(group, currentUser);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, user)
            .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        if (member.getStatus() == GroupMemberStatus.APPROVED) {
            return mapToMemberDTO(member);
        }

        member.setStatus(GroupMemberStatus.APPROVED);
        GroupMember saved = groupMemberRepository.save(member);

        // Update member count when approving
        group.setMembersCount(group.getMembersCount() + 1);
        groupRepository.save(group);

        notificationService.createNotification(
            user,
            "Groupe",
            "Votre demande pour rejoindre le groupe a été approuvée.",
            NotificationType.GROUP_JOIN,
            "GROUP",
            group.getId()
        );

        auditService.logAction("APPROVE_MEMBER", "GROUP", groupId, "Membre approuvé: " + user.getEmail());
        broadcastGroupMembers(groupId);
        return mapToMemberDTO(saved);
        }

        @Override
        @Transactional
        public void rejectMember(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

            // Only admins/owners can reject
            User currentUser = getCurrentUserEntity();
            checkGroupAdmin(group, currentUser);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, user)
            .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        // Only remove pending entries on reject
        if (member.getStatus() == GroupMemberStatus.PENDING) {
            groupMemberRepository.delete(member);

            // Do not decrement membersCount because pending were not counted
            notificationService.createNotification(
                user,
                "Groupe",
                "Votre demande pour rejoindre le groupe a été rejetée.",
                NotificationType.GROUP_REJECTED,
                "GROUP",
                group.getId()
            );

            auditService.logAction("REJECT_MEMBER", "GROUP", groupId, "Membre rejeté: " + user.getEmail());
            broadcastGroupMembers(groupId);
        }
        }

    @Override
    @Transactional
    public void removeMember(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, userToRemove)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        groupMemberRepository.delete(member);

        // Update member count
        group.setMembersCount(group.getMembersCount() - 1);
        groupRepository.save(group);
        broadcastGroupMembers(groupId);
    }

    @Override
    @Transactional
    public void leaveGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        User currentUser = getCurrentUserEntity();

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new RuntimeException("Vous n'êtes pas membre de ce groupe"));

        groupMemberRepository.delete(member);

        // Update member count
        group.setMembersCount(group.getMembersCount() - 1);
        groupRepository.save(group);

        auditService.logAction("LEAVE_GROUP", "GROUP", groupId, "Utilisateur a quitté le groupe");
        broadcastGroupMembers(groupId);
    }

    @Override
    @Transactional
    public GroupMemberDTO joinGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        User currentUser = getCurrentUserEntity();

        // Check if user is already a member
        if (groupMemberRepository.existsByGroupAndUser(group, currentUser)) {
            throw new RuntimeException("Vous êtes déjà membre de ce groupe");
        }
        // If the group is public, join immediately; otherwise create a pending request
        GroupMemberStatus statusToSet = group.getPrivacy() == GroupPrivacy.PUBLIC ? GroupMemberStatus.APPROVED : GroupMemberStatus.PENDING;

        GroupMember member = addMemberToGroupEntity(group, currentUser, GroupMemberRole.MEMBER, statusToSet);

        if (statusToSet == GroupMemberStatus.APPROVED) {
            // Notify user of successful join
            notificationService.createNotification(
                    currentUser,
                    "Groupe rejoint",
                    "Vous avez rejoint le groupe : " + group.getGroupName(),
                    NotificationType.GROUP_JOIN,
                    "GROUP",
                    group.getId());

            auditService.logAction("JOIN_GROUP", "GROUP", groupId, "Utilisateur a rejoint le groupe");
        } else {
            // Notify group owner/admins about pending request
            User owner = group.getCreator();
            if (owner != null) {
                notificationService.createNotification(
                        owner,
                        "Nouvelle demande de groupe",
                        currentUser.getFirstName() + " " + currentUser.getLastName() + " a demandé à rejoindre le groupe : " + group.getGroupName(),
                        NotificationType.GROUP_INVITE,
                        "GROUP",
                        group.getId());
            }

            auditService.logAction("REQUEST_JOIN_GROUP", "GROUP", groupId, "Utilisateur a demandé à rejoindre (pending): " + currentUser.getEmail());
        }

        GroupMemberDTO memberDTO = mapToMemberDTO(member);
        broadcastGroupMembers(groupId);
        return memberDTO;
    }

    @Override
    public List<GroupMemberDTO> getGroupMembers(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));

        return groupMemberRepository.findByGroup(group).stream()
                .map(this::mapToMemberDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupDTO> getUserGroups(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return groupMemberRepository.findByUser(user).stream()
                .map(m -> mapToDTO(m.getGroup()))
                .collect(Collectors.toList());
    }

    private GroupMember addMemberToGroupEntity(Group group, User user, GroupMemberRole role, GroupMemberStatus status) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(status);

        GroupMember savedMember = groupMemberRepository.save(member);

        // Update member count only if approved immediately
        if (status == GroupMemberStatus.APPROVED) {
            group.setMembersCount(group.getMembersCount() + 1);
            groupRepository.save(group);
        }

        return savedMember;
    }

    private void checkGroupAdmin(Group group, User user) {
        GroupMember member = groupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new RuntimeException("Vous n'êtes pas membre de ce groupe"));

        if (member.getRole() != GroupMemberRole.ADMIN) {
            throw new RuntimeException("Action non autorisée : Vous n'êtes pas administrateur du groupe");
        }
    }

    private GroupDTO mapToDTO(Group group) {
        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setGroupName(group.getGroupName());
        dto.setDescription(group.getDescription());
        dto.setPrivacy(group.getPrivacy());
        dto.setStatus(group.getStatus());
        dto.setWebsite(group.getWebsite());
        dto.setLogoUrl(group.getLogoUrl());
        dto.setBannerUrl(group.getBannerUrl());
        dto.setMembersCount(group.getMembersCount());
        dto.setCreatorId(group.getCreator().getId());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());

        if (group.getMemberCriteria() != null) {
            dto.setLocation(group.getMemberCriteria().getLocation());
            dto.setAffiliation(group.getMemberCriteria().getAffiliation());
            dto.setFieldOfStudy(group.getMemberCriteria().getFieldOfStudy());
            dto.setDegree(group.getMemberCriteria().getDegree());
            dto.setGraduationYear(group.getMemberCriteria().getGraduationYear());
            dto.setInstitutionProgram(group.getMemberCriteria().getInstitutionProgram());
            dto.setOtherDegree(group.getMemberCriteria().getOtherDegree());
            dto.setOtherGraduationYear(group.getMemberCriteria().getOtherGraduationYear());
            dto.setCompany(group.getMemberCriteria().getCompany());
            dto.setIndustry(group.getMemberCriteria().getIndustry());
            dto.setJobFunction(group.getMemberCriteria().getJobFunction());
            dto.setWillingOffering(group.getMemberCriteria().getWillingOffering());
            dto.setWillingSeeking(group.getMemberCriteria().getWillingSeeking());
            dto.setMentoringOffering(group.getMemberCriteria().getMentoringOffering());
            dto.setMentoringSeeking(group.getMemberCriteria().getMentoringSeeking());
        }
        dto.setLabels(group.getLabels());
        dto.setTagging(group.getTagging());
        return dto;
    }

    private void broadcastGroupMembers(UUID groupId) {
        try {
            List<GroupMemberDTO> members = getGroupMembers(groupId);
            messagingTemplate.convertAndSend("/topic/group/" + groupId + "/members", members);
            log.info("Broadcasted updated group members to WebSocket for group: {}", groupId);
        } catch (Exception e) {
            log.error("Error broadcasting group members for group " + groupId, e);
        }
    }

    private GroupMemberDTO mapToMemberDTO(GroupMember member) {
        GroupMemberDTO dto = new GroupMemberDTO();
        dto.setId(member.getId());
        dto.setGroupId(member.getGroup().getId());
        dto.setUserId(member.getUser().getId());
        dto.setUserFullName(member.getUser().getFirstName() + " " + member.getUser().getLastName());
        dto.setRole(member.getRole());
        dto.setIsManual(member.getIsManual());
        dto.setJoinedAt(member.getJoinedAt());
        dto.setStatus(member.getStatus() != null ? member.getStatus().name() : null);
        
        // Populate additional user profile details
        if (member.getUser() != null) {
            dto.setFirstName(member.getUser().getFirstName());
            dto.setLastName(member.getUser().getLastName());
            dto.setAvatarUrl(member.getUser().getAvatarUrl());
            dto.setIsOnline(member.getUser().getIsOnline());
            if (member.getUser().getRole() != null) {
                dto.setUserRole(member.getUser().getRole().name());
            }
        }
        return dto;
    }
}
