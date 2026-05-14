package tn.esprit.espritconnectbackend.service;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.GroupDTO;
import tn.esprit.espritconnectbackend.dto.GroupMemberDTO;
import tn.esprit.espritconnectbackend.entities.enums.GroupMemberRole;
import tn.esprit.espritconnectbackend.entities.enums.GroupStatus;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface GroupService {
    GroupDTO createGroup(GroupDTO groupDTO);
    GroupDTO createGroupWithFiles(GroupDTO groupDTO, MultipartFile logoFile, MultipartFile bannerFile) throws IOException;
    GroupDTO updateGroup(UUID groupId, GroupDTO groupDTO);
    GroupDTO updateGroupWithFiles(UUID groupId, GroupDTO groupDTO, MultipartFile logoFile, MultipartFile bannerFile) throws IOException;
    void deleteGroup(UUID groupId);
    GroupDTO getGroupById(UUID groupId);
    List<GroupDTO> getAllGroups();
    List<GroupDTO> getPendingGroups();
    GroupDTO approveGroup(UUID groupId);
    GroupDTO rejectGroup(UUID groupId);
    
    // Member management
    GroupMemberDTO addMember(UUID groupId, UUID userId, GroupMemberRole role);
    void removeMember(UUID groupId, UUID userId);
    void leaveGroup(UUID groupId);
    GroupMemberDTO joinGroup(UUID groupId);
    List<GroupMemberDTO> getGroupMembers(UUID groupId);
    List<GroupDTO> getUserGroups(UUID userId);
}
