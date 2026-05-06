package tn.esprit.espritconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "other_education")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherEducation {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "institution_name", length = 200)
    private String institutionName;

    @Column(length = 100)
    private String degree;

    @Column(name = "graduation_year")
    private Integer graduationYear;
}
