package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.GroupDTO;
import tn.esprit.espritconnectbackend.dto.GroupMemberDTO;
import tn.esprit.espritconnectbackend.entities.Group;
import tn.esprit.espritconnectbackend.entities.GroupMember;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.GroupMemberRole;
import tn.esprit.espritconnectbackend.entities.enums.NotificationType;
import tn.esprit.espritconnectbackend.repositories.GroupMemberRepository;
import tn.esprit.espritconnectbackend.repositories.GroupRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

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
        group.setCreator(creator);
        group.setWebsite(groupDTO.getWebsite());
        group.setLogoUrl(groupDTO.getLogoUrl());
        group.setBannerUrl(groupDTO.getBannerUrl());
        
        Group savedGroup = groupRepository.save(group);
        
        // Add creator as ADMIN member
        addMemberToGroupEntity(savedGroup, creator, GroupMemberRole.ADMIN);
        
        auditService.logAction("CREATE_GROUP", "GROUP", savedGroup.getId(), "Groupe créé : " + savedGroup.getGroupName());
        
        return mapToDTO(savedGroup);
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
        group.setLogoUrl(groupDTO.getLogoUrl());
        group.setBannerUrl(groupDTO.getBannerUrl());
        
        Group updatedGroup = groupRepository.save(group);
        auditService.logAction("UPDATE_GROUP", "GROUP", updatedGroup.getId(), "Groupe mis à jour");
        
        return mapToDTO(updatedGroup);
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
                .map(this::mapToDTO)
                .collect(Collectors.toList());
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
        
        GroupMember member = addMemberToGroupEntity(group, userToAdd, role);
        
        // Notify user
        notificationService.createNotification(
            userToAdd, 
            "Nouveau groupe", 
            "Vous avez été ajouté au groupe : " + group.getGroupName(), 
            NotificationType.GROUP_INVITE, 
            "GROUP",
            group.getId()
        );
        
        return mapToMemberDTO(member);
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

    private GroupMember addMemberToGroupEntity(Group group, User user, GroupMemberRole role) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        
        GroupMember savedMember = groupMemberRepository.save(member);
        
        // Update member count
        group.setMembersCount(group.getMembersCount() + 1);
        groupRepository.save(group);
        
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
        dto.setWebsite(group.getWebsite());
        dto.setLogoUrl(group.getLogoUrl());
        dto.setBannerUrl(group.getBannerUrl());
        dto.setMembersCount(group.getMembersCount());
        dto.setCreatorId(group.getCreator().getId());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        return dto;
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
        return dto;
    }
}
