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
            @RequestParam("groupName") String groupName,
            @RequestParam("description") String description,
            @RequestParam(value = "website", required = false) String website,
            @RequestParam(value = "privacy", required = false) String privacy,
            @RequestParam(value = "tagging", required = false) String tagging,
            @RequestParam(value = "labels", required = false) String labels,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "affiliation", required = false) String affiliation,
            @RequestParam(value = "fieldOfStudy", required = false) String fieldOfStudy,
            @RequestParam(value = "degree", required = false) String degree,
            @RequestParam(value = "graduationYear", required = false) String graduationYear,
            @RequestParam(value = "institutionProgram", required = false) String institutionProgram,
            @RequestParam(value = "otherDegree", required = false) String otherDegree,
            @RequestParam(value = "otherGraduationYear", required = false) String otherGraduationYear,
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "industry", required = false) String industry,
            @RequestParam(value = "jobFunction", required = false) String jobFunction,
            @RequestParam(value = "willingOffering", required = false) String willingOffering,
            @RequestParam(value = "willingSeeking", required = false) String willingSeeking,
            @RequestParam(value = "mentoringOffering", required = false) String mentoringOffering,
            @RequestParam(value = "mentoringSeeking", required = false) String mentoringSeeking,
            @RequestParam(value = "addMembers", required = false) String addMembers,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestParam(value = "bannerFile", required = false) MultipartFile bannerFile) throws IOException {

        GroupDTO groupDTO = new GroupDTO();
        groupDTO.setGroupName(groupName);
        groupDTO.setDescription(description);
        groupDTO.setWebsite(website);
        try {
            groupDTO.setPrivacy(tn.esprit.espritconnectbackend.entities.enums.GroupPrivacy
                    .valueOf(privacy != null && !privacy.isEmpty() ? privacy.toUpperCase() : "PUBLIC"));
        } catch (IllegalArgumentException e) {
            groupDTO.setPrivacy(tn.esprit.espritconnectbackend.entities.enums.GroupPrivacy.PUBLIC);
        }
        groupDTO.setTagging(Boolean.valueOf(tagging));
        groupDTO.setLabels(labels);
        groupDTO.setLocation(location);
        groupDTO.setAffiliation(affiliation);
        groupDTO.setFieldOfStudy(fieldOfStudy);
        groupDTO.setDegree(degree);
        if (graduationYear != null && !graduationYear.isEmpty()) {
            try {
                groupDTO.setGraduationYear(Integer.valueOf(graduationYear));
            } catch (NumberFormatException e) {
                // Ignore or log error
            }
        }
        groupDTO.setInstitutionProgram(institutionProgram);
        groupDTO.setOtherDegree(otherDegree);
        if (otherGraduationYear != null && !otherGraduationYear.isEmpty()) {
            try {
                groupDTO.setOtherGraduationYear(Integer.valueOf(otherGraduationYear));
            } catch (NumberFormatException e) {
                // Ignore or log error
            }
        }
        groupDTO.setCompany(company);
        groupDTO.setIndustry(industry);
        groupDTO.setJobFunction(jobFunction);
        groupDTO.setWillingOffering(willingOffering);
        groupDTO.setWillingSeeking(willingSeeking);
        groupDTO.setMentoringOffering(mentoringOffering);
        groupDTO.setMentoringSeeking(mentoringSeeking);
        groupDTO.setAddMembers(addMembers);

        return ResponseEntity.ok(groupService.createGroupWithFiles(groupDTO, logoFile, bannerFile));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupDTO> updateGroup(@PathVariable UUID id, @RequestBody GroupDTO groupDTO) {
        return ResponseEntity.ok(groupService.updateGroup(id, groupDTO));
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
