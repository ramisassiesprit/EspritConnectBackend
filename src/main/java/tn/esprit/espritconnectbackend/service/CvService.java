package tn.esprit.espritconnectbackend.service;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.entities.User;

import java.util.UUID;

public interface CvService {
    User uploadCv(UUID userId, MultipartFile file);
}
