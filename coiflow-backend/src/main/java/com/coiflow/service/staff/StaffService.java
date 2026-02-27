package com.coiflow.service.staff;

import com.coiflow.dto.staff.CreateStaffRequest;
import com.coiflow.dto.staff.StaffResponse;
import com.coiflow.dto.staff.UpdateStaffRequest;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.salon.Salon;
import com.coiflow.model.user.Barber;
import com.coiflow.model.user.Utilisateur;
import com.coiflow.repository.salon.SalonRepository;
import com.coiflow.repository.user.UtilisateurRepository;
import com.coiflow.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final UtilisateurRepository utilisateurRepository;
    private final SalonRepository salonRepository;
    private final PasswordEncoder passwordEncoder;

    private String requireSalonId() {
        String salonId = TenantContextHolder.getSalonId();
        if (salonId == null || salonId.isBlank()) {
            throw new IllegalStateException("Contexte de salon manquant");
        }
        return salonId;
    }

    @PreAuthorize("hasRole('MANAGER')")
    public List<StaffResponse> getStaff() {
        String salonId = requireSalonId();
        return utilisateurRepository.findBySalonIdAndTypeAndActive(salonId, Barber.class, true)
                .stream()
                .map(u -> toResponse((Barber) u))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public StaffResponse createStaff(CreateStaffRequest request) {
        String salonId = requireSalonId();

        if (utilisateurRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new IllegalStateException("Un compte avec cet email existe deja");
        }

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable"));

        Barber barber = new Barber();
        barber.setId(UUID.randomUUID().toString());
        barber.setSalon(salon);
        barber.setFirstName(request.getFirstName());
        barber.setLastName(request.getLastName());
        barber.setEmail(request.getEmail().toLowerCase().trim());
        barber.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        barber.setCommissionRate(request.getCommissionRate());
        barber.setActive(true);

        utilisateurRepository.save(barber);
        return toResponse(barber);
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public StaffResponse updateStaff(String id, UpdateStaffRequest request) {
        String salonId = requireSalonId();
        Utilisateur u = utilisateurRepository.findByIdAndType(id, Barber.class)
                .filter(b -> salonId.equals(b.getSalonId()))
                .orElseThrow(() -> new ResourceNotFoundException("Coiffeur introuvable"));

        Barber barber = (Barber) u;
        barber.setFirstName(request.getFirstName());
        barber.setLastName(request.getLastName());
        barber.setCommissionRate(request.getCommissionRate());

        return toResponse((Barber) utilisateurRepository.save(barber));
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteStaff(String id) {
        String salonId = requireSalonId();
        Utilisateur u = utilisateurRepository.findByIdAndType(id, Barber.class)
                .filter(b -> salonId.equals(b.getSalonId()))
                .orElseThrow(() -> new ResourceNotFoundException("Coiffeur introuvable"));
        u.setActive(false);
        utilisateurRepository.save(u);
    }

    private StaffResponse toResponse(Barber barber) {
        return StaffResponse.builder()
                .id(barber.getId())
                .firstName(barber.getFirstName())
                .lastName(barber.getLastName())
                .email(barber.getEmail())
                .commissionRate(barber.getCommissionRate())
                .active(barber.isActive())
                .createdAt(barber.getCreatedAt() != null ? barber.getCreatedAt().toString() : null)
                .build();
    }
}
