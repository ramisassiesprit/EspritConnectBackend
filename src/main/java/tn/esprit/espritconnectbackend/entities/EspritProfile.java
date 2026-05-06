package tn.esprit.espritconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "esprit_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspritProfile {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "student_number", length = 50)
    private String studentNumber;

    @Column(name = "field_of_study", length = 150)
    private String fieldOfStudy;

    @Column(length = 100)
    private String degree;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(length = 150)
    private String program;

    @Column(length = 150)
    private String institution;
}
