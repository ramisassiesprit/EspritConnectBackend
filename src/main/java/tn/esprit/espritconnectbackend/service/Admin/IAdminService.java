package tn.esprit.espritconnectbackend.service.Admin;

import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;

import java.util.List;
import java.util.UUID;

public interface IAdminService {
    List<UserDTO> getUsersByRole(UserRole role);
    void updateUserStatus(UUID userId, UserStatus status);
}
