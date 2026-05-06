package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.enums.UserRole;
import tn.esprit.espritconnectbackend.entities.enums.UserStatus;

import java.util.List;
import java.util.UUID;

public interface UserService {
    // Profil utilisateur
    UserDTO getCurrentUser();
    UserDTO updateProfile(UserDTO userDTO);
    
    // Admin CRUD
    List<UserDTO> getAllUsers();
    List<UserDTO> getUsersByRole(UserRole role);
    void updateUserStatus(UUID userId, UserStatus status);
    void deleteUser(UUID userId);
}
