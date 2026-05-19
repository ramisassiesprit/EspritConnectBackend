package tn.esprit.espritconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.GroupDTO;
import tn.esprit.espritconnectbackend.dto.GroupMemberDTO;
import tn.esprit.espritconnectbackend.entities.enums.GroupMemberRole;
import tn.esprit.espritconnectbackend.service.GroupService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupDTO> createGroup(@RequestBody GroupDTO groupDTO) {
        return ResponseEntity.ok(groupService.createGroup(groupDTO));
    }

    @PostMapping(value = "/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupDTO> createGroupWithFiles(
            @RequestPart("group") GroupDTO groupDTO,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile) throws IOException {

        return ResponseEntity.ok(groupService.createGroupWithFiles(groupDTO, logoFile, bannerFile));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupDTO> updateGroup(
            @PathVariable UUID id,
            @RequestPart("group") GroupDTO groupDTO,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile) throws IOException {
        return ResponseEntity.ok(groupService.updateGroupWithFiles(id, groupDTO, logoFile, bannerFile));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GroupDTO> updateGroupJson(
            @PathVariable UUID id,
            @RequestBody GroupDTO groupDTO) {
        return ResponseEntity.ok(groupService.updateGroup(id, groupDTO));
    }
 
    @PostMapping(value = "/{id}/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupDTO> updateGroupWithFilesPost(
            @PathVariable UUID id,
            @RequestPart("group") GroupDTO groupDTO,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile) throws IOException {
        try {
            System.out.println("Received multipart update for group id=" + id + ", groupName=" + groupDTO.getGroupName()
                    + ", logoFile=" + (logoFile != null ? logoFile.getOriginalFilename() : "null")
                    + ", bannerFile=" + (bannerFile != null ? bannerFile.getOriginalFilename() : "null"));
            GroupDTO result = groupService.updateGroupWithFiles(id, groupDTO, logoFile, bannerFile);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDTO> getGroup(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.getGroupById(id));
    }

    @GetMapping
    public ResponseEntity<List<GroupDTO>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GroupDTO>> getPendingGroups() {
        return ResponseEntity.ok(groupService.getPendingGroups());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroupDTO> approveGroup(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.approveGroup(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroupDTO> rejectGroup(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.rejectGroup(id));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroupDTO> setStatus(@PathVariable UUID id, @RequestParam tn.esprit.espritconnectbackend.entities.enums.GroupStatus status) {
        return ResponseEntity.ok(groupService.setGroupStatus(id, status));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupMemberDTO> addMember(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @RequestParam GroupMemberRole role) {
        return ResponseEntity.ok(groupService.addMember(id, userId, role));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        groupService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable UUID id) {
        groupService.leaveGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<GroupMemberDTO> joinGroup(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.joinGroup(id));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<GroupMemberDTO>> getGroupMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.getGroupMembers(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupDTO>> getUserGroups(@PathVariable UUID userId) {
        return ResponseEntity.ok(groupService.getUserGroups(userId));
    }
}
