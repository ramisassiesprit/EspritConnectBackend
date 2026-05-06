package tn.esprit.espritconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "willing_to_help")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WillingToHelp {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String offering;

    @Column(length = 255)
    private String seeking;
}
