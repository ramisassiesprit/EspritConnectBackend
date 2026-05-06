package tn.esprit.espritconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "group_member_criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberCriteria {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private Group group;

    @Column(length = 150)
    private String location;

    @Column(length = 150)
    private String affiliation;

    @Column(name = "field_of_study", length = 150)
    private String fieldOfStudy;

    @Column(length = 100)
    private String degree;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "institution_program", length = 200)
    private String institutionProgram;

    @Column(name = "other_degree", length = 100)
    private String otherDegree;

    @Column(name = "other_graduation_year")
    private Integer otherGraduationYear;

    @Column(length = 200)
    private String company;

    @Column(length = 150)
    private String industry;

    @Column(name = "job_function", length = 150)
    private String jobFunction;

    @Column(name = "willing_offering", length = 255)
    private String willingOffering;

    @Column(name = "willing_seeking", length = 255)
    private String willingSeeking;

    @Column(name = "mentoring_offering", length = 255)
    private String mentoringOffering;

    @Column(name = "mentoring_seeking", length = 255)
    private String mentoringSeeking;
}
