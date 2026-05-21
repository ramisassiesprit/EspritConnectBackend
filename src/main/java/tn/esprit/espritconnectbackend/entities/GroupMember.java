package tn.esprit.espritconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import tn.esprit.espritconnectbackend.entities.enums.GroupMemberRole;
import tn.esprit.espritconnectbackend.entities.enums.GroupMemberStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "group_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GroupMemberRole role = GroupMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private GroupMemberStatus status = GroupMemberStatus.APPROVED;

    @Column(name = "is_manual")
    @Builder.Default
    private Boolean isManual = false;

    @Column(name = "joined_at", updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
}
