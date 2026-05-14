package tn.esprit.espritconnectbackend.dto;

import lombok.Data;
import tn.esprit.espritconnectbackend.entities.enums.GroupPrivacy;
import tn.esprit.espritconnectbackend.entities.enums.GroupStatus;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GroupDTO {
    private UUID id;
    private String groupName;
    private String description;
    private String website;
    private String logoUrl;
    private String bannerUrl;
    private GroupPrivacy privacy;
    private GroupStatus status;
    private Boolean tagging;
    private String labels;
    private String location;
    private String affiliation;
    private String fieldOfStudy;
    private String degree;
    private String graduationYear;
    private String institutionProgram;
    private String otherDegree;
    private String otherGraduationYear;
    private String company;
    private String industry;
    private String jobFunction;
    private String willingOffering;
    private String willingSeeking;
    private String mentoringOffering;
    private String mentoringSeeking;
    private String addMembers;
    private Integer membersCount;
    private UUID creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
