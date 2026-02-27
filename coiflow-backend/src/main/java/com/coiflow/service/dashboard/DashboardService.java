package com.coiflow.service.dashboard;

import com.coiflow.dto.dashboard.DailyRevenueResponse;
import com.coiflow.dto.dashboard.DashboardStatsResponse;
import com.coiflow.model.enums.AppointmentStatus;
import com.coiflow.model.enums.TransactionStatus;
import com.coiflow.model.transaction.Transaction;
import com.coiflow.model.user.Barber;
import com.coiflow.repository.appointment.AppointmentRepository;
import com.coiflow.repository.transaction.TransactionRepository;
import com.coiflow.repository.user.UtilisateurRepository;
import com.coiflow.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Algiers");

    private final AppointmentRepository appointmentRepository;
    private final TransactionRepository transactionRepository;
    private final UtilisateurRepository utilisateurRepository;

    private String requireSalonId() {
        String salonId = TenantContextHolder.getSalonId();
        if (salonId == null || salonId.isBlank()) {
            throw new IllegalStateException("Contexte de salon manquant");
        }
        return salonId;
    }

    @PreAuthorize("hasRole('MANAGER')")
    public DashboardStatsResponse getStats() {
        String salonId = requireSalonId();
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        // Revenue today: sum of COMPLETED transactions
        var transactions = transactionRepository.findBySalonIdAndCreatedAtBetween(salonId, dayStart, dayEnd);
        BigDecimal revenueToday = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Appointments today by status
        var appointments = appointmentRepository.findBySalonIdAndStartTimeBetween(salonId, dayStart, dayEnd);
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (AppointmentStatus s : AppointmentStatus.values()) {
            byStatus.put(s.name(), 0);
        }
        for (var a : appointments) {
            byStatus.merge(a.getStatus().name(), 1, Integer::sum);
        }

        // Active barbers
        int activeBarbersCount = utilisateurRepository
                .findBySalonIdAndTypeAndActive(salonId, Barber.class, true).size();

        return DashboardStatsResponse.builder()
                .revenueToday(revenueToday)
                .appointmentsToday(appointments.size())
                .appointmentsByStatus(byStatus)
                .activeBarbersCount(activeBarbersCount)
                .build();
    }

    @PreAuthorize("hasRole('MANAGER')")
    public List<DailyRevenueResponse> getRevenue(LocalDate start, LocalDate end) {
        String salonId = requireSalonId();
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay();

        var transactions = transactionRepository.findBySalonIdAndCreatedAtBetween(salonId, startDt, endDt);

        // Group completed transactions by day
        Map<LocalDate, List<Transaction>> byDay = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Build response with all dates in range (including zero-revenue days)
        List<DailyRevenueResponse> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            var dayTxns = byDay.getOrDefault(d, List.of());
            BigDecimal revenue = dayTxns.stream()
                    .map(Transaction::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(DailyRevenueResponse.builder()
                    .date(d.toString())
                    .revenue(revenue)
                    .transactionCount(dayTxns.size())
                    .build());
        }
        return result;
    }
}
