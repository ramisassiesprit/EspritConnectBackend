package tn.esprit.espritconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "newsletter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Newsletter {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /** JSON string or filter expression for target audience */
    @Column(name = "target_filter", columnDefinition = "TEXT")
    private String targetFilter;

    @Column(name = "sent_count")
    @Builder.Default
    private Integer sentCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
