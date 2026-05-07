package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.GroupDTO;
import tn.esprit.espritconnectbackend.dto.GroupMemberDTO;
import tn.esprit.espritconnectbackend.entities.enums.GroupMemberRole;

import java.util.List;
import java.util.UUID;

public interface GroupService {
    GroupDTO createGroup(GroupDTO groupDTO);
    GroupDTO updateGroup(UUID groupId, GroupDTO groupDTO);
    void deleteGroup(UUID groupId);
    GroupDTO getGroupById(UUID groupId);
    List<GroupDTO> getAllGroups();
    
    // Member management
    GroupMemberDTO addMember(UUID groupId, UUID userId, GroupMemberRole role);
    void removeMember(UUID groupId, UUID userId);
    List<GroupMemberDTO> getGroupMembers(UUID groupId);
    List<GroupDTO> getUserGroups(UUID userId);
}
