package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.dto.WillingToHelpDTO;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.entities.WillingToHelp;
import tn.esprit.espritconnectbackend.repositories.UserRepository;
import tn.esprit.espritconnectbackend.repositories.WillingToHelpRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WillingToHelpServiceImpl implements WillingToHelpService {

    private final WillingToHelpRepository willingToHelpRepository;
    private final UserRepository userRepository;


    private User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    @Override
    public WillingToHelpDTO create(WillingToHelpDTO willingToHelpDTO) {
        WillingToHelp willingToHelp = WillingToHelp.builder()
                .user(getCurrentUserEntity())
                .offerHelp(willingToHelpDTO.getOfferHelp())
                .seekHelp(willingToHelpDTO.getSeekHelp())
                .offerMentor(willingToHelpDTO.getOfferMentor())
                .seekMentor(willingToHelpDTO.getSeekMentor())
                .build();

        WillingToHelp saved = willingToHelpRepository.save(willingToHelp);
        return mapToDTO(saved);
    }

    @Override
    public WillingToHelpDTO update(UUID id, WillingToHelpDTO willingToHelpDTO) {
        WillingToHelp willingToHelp = willingToHelpRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WillingToHelp non trouvé"));

        if (willingToHelpDTO.getOfferHelp() != null) {
            willingToHelp.setOfferHelp(willingToHelpDTO.getOfferHelp());
        }
        if (willingToHelpDTO.getSeekHelp() != null) {
            willingToHelp.setSeekHelp(willingToHelpDTO.getSeekHelp());
        }
        if (willingToHelpDTO.getOfferMentor() != null) {
            willingToHelp.setOfferMentor(willingToHelpDTO.getOfferMentor());
        }
        if (willingToHelpDTO.getSeekMentor() != null) {
            willingToHelp.setSeekMentor(willingToHelpDTO.getSeekMentor());
        }

        WillingToHelp updated = willingToHelpRepository.save(willingToHelp);
        return mapToDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public WillingToHelpDTO getById(UUID id) {
        WillingToHelp willingToHelp = willingToHelpRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WillingToHelp non trouvé"));
        return mapToDTO(willingToHelp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WillingToHelpDTO> getByUserId(UUID userId) {
        return willingToHelpRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        willingToHelpRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WillingToHelpDTO> getAll() {
        return willingToHelpRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private WillingToHelpDTO mapToDTO(WillingToHelp willingToHelp) {
        WillingToHelpDTO dto = new WillingToHelpDTO();
        dto.setId(willingToHelp.getId());
        dto.setOfferHelp(willingToHelp.getOfferHelp());
        dto.setSeekHelp(willingToHelp.getSeekHelp());
        dto.setOfferMentor(willingToHelp.getOfferMentor());
        dto.setSeekMentor(willingToHelp.getSeekMentor());
        return dto;
    }
}
