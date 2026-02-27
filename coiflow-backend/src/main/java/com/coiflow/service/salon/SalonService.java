package com.coiflow.service.salon;

import com.coiflow.dto.salon.CreateSalonRequest;
import com.coiflow.dto.salon.SalonResponse;
import com.coiflow.dto.salon.UpdateSalonRequest;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.mapper.SalonMapper;
import com.coiflow.model.salon.Salon;
import com.coiflow.model.user.Manager;
import com.coiflow.repository.salon.SalonRepository;
import com.coiflow.repository.user.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalonService {

    private final SalonRepository salonRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final SalonMapper salonMapper;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public SalonResponse createSalon(CreateSalonRequest request) {
        if (utilisateurRepository.existsByEmail(request.getManagerEmail().toLowerCase().trim())) {
            throw new IllegalStateException("Un compte avec cet email existe deja");
        }

        Salon salon = Salon.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .active(true)
                .build();
        salonRepository.save(salon);

        Manager manager = new Manager();
        manager.setId(UUID.randomUUID().toString());
        manager.setSalon(salon);
        manager.setFirstName(request.getManagerFirstName());
        manager.setLastName(request.getManagerLastName());
        manager.setEmail(request.getManagerEmail().toLowerCase().trim());
        manager.setPasswordHash(passwordEncoder.encode(request.getManagerPassword()));
        manager.setActive(true);
        utilisateurRepository.save(manager);

        SalonResponse response = salonMapper.toResponse(salon);
        response.setManagerId(manager.getId());
        response.setManagerName(manager.getFirstName() + " " + manager.getLastName());
        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<SalonResponse> getAllSalons() {
        return salonRepository.findAll().stream()
                .map(salon -> {
                    SalonResponse r = salonMapper.toResponse(salon);
                    utilisateurRepository.findBySalonIdAndTypeAndActive(salon.getId(), Manager.class, true)
                            .stream().findFirst().ifPresent(m -> {
                                r.setManagerId(m.getId());
                                r.setManagerName(m.getFirstName() + " " + m.getLastName());
                            });
                    return r;
                })
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public SalonResponse updateSalon(String id, UpdateSalonRequest request) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable"));
        salon.setName(request.getName());
        salon.setAddress(request.getAddress());
        salon.setPhone(request.getPhone());
        salon.setEmail(request.getEmail());

        SalonResponse response = salonMapper.toResponse(salonRepository.save(salon));
        utilisateurRepository.findBySalonIdAndTypeAndActive(id, Manager.class, true)
                .stream().findFirst().ifPresent(m -> {
                    response.setManagerId(m.getId());
                    response.setManagerName(m.getFirstName() + " " + m.getLastName());
                });
        return response;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public SalonResponse toggleActive(String id) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable"));
        salon.setActive(!salon.isActive());

        SalonResponse response = salonMapper.toResponse(salonRepository.save(salon));
        utilisateurRepository.findBySalonIdAndTypeAndActive(id, Manager.class, true)
                .stream().findFirst().ifPresent(m -> {
                    response.setManagerId(m.getId());
                    response.setManagerName(m.getFirstName() + " " + m.getLastName());
                });
        return response;
    }
}
