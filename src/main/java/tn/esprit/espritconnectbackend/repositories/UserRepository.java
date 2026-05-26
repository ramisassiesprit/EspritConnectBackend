package tn.esprit.espritconnectbackend.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByIsOnlineTrue();
    Optional<User> findByCode(String code);
    Optional<User> findByResetPasswordToken(String token);
    long countByRoleIn(List<UserRole> roles);

    @EntityGraph(attributePaths = {"espritProfile", "skills"})
    @Query("""
            select distinct u
            from User u
            where u.id <> :currentUserId
            """)
    List<User> findMentorCandidatesForMatching(@Param("currentUserId") UUID currentUserId);
}
