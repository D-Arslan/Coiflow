package com.coiflow.service.commission;

import com.coiflow.dto.commission.CommissionResponse;
import com.coiflow.model.commission.Commission;
import com.coiflow.model.user.Utilisateur;
import com.coiflow.repository.commission.CommissionRepository;
import com.coiflow.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRepository commissionRepository;

    private String requireSalonId() {
        String salonId = TenantContextHolder.getSalonId();
        if (salonId == null || salonId.isBlank()) {
            throw new IllegalStateException("Contexte de salon manquant");
        }
        return salonId;
    }

    @PreAuthorize("hasAnyRole('MANAGER','BARBER')")
    public List<CommissionResponse> getAll(LocalDate start, LocalDate end, String barberId) {
        String salonId = requireSalonId();
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay();

        // For barbers: force barberId to current user (security)
        Utilisateur currentUser = (Utilisateur) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if ("BARBER".equals(currentUser.getRole().name())) {
            barberId = currentUser.getId();
        }

        List<Commission> commissions;
        if (barberId != null && !barberId.isBlank()) {
            commissions = commissionRepository.findByBarberIdAndCreatedAtBetween(barberId, startDt, endDt);
        } else {
            commissions = commissionRepository.findBySalonIdAndCreatedAtBetween(salonId, startDt, endDt);
        }

        return commissions.stream().map(this::toResponse).toList();
    }

    private CommissionResponse toResponse(Commission c) {
        return CommissionResponse.builder()
                .id(c.getId())
                .barberId(c.getBarber().getId())
                .barberName(c.getBarber().getFirstName() + " " + c.getBarber().getLastName())
                .transactionId(c.getTransaction().getId())
                .rateApplied(c.getRateApplied())
                .amount(c.getAmount())
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .build();
    }
}
