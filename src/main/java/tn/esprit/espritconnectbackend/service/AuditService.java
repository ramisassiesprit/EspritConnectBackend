package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.esprit.espritconnectbackend.entities.AuditLog;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.repositories.AuditLogRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void logAction(String action, String targetType, UUID targetId, String metadata) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);

        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .metadata(metadata)
                .build();

        auditLogRepository.save(log);
    }
}
