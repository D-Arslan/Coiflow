package com.coiflow.service.dashboard;

import com.coiflow.TestSecurityUtils;
import com.coiflow.dto.dashboard.DailyRevenueResponse;
import com.coiflow.dto.dashboard.DashboardStatsResponse;
import com.coiflow.model.appointment.Appointment;
import com.coiflow.model.enums.AppointmentStatus;
import com.coiflow.model.enums.TransactionStatus;
import com.coiflow.model.transaction.Transaction;
import com.coiflow.model.user.Barber;
import com.coiflow.model.user.Manager;
import com.coiflow.repository.appointment.AppointmentRepository;
import com.coiflow.repository.transaction.TransactionRepository;
import com.coiflow.repository.user.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.coiflow.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UtilisateurRepository utilisateurRepository;

    @InjectMocks private DashboardService dashboardService;

    private Manager manager;

    @BeforeEach
    void setUp() {
        TestSecurityUtils.setTenantContext(SALON_ID);
        manager = aManager(SALON_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAll();
    }

    @Test
    void getStats_revenueToday() {
        // 1 COMPLETED transaction (1500) + 1 VOIDED (should be excluded)
        Appointment a1 = anAppointment(AppointmentStatus.COMPLETED, SALON_ID);
        Transaction completed = aTransaction(a1, manager);

        Appointment a2 = anAppointment(AppointmentStatus.COMPLETED, SALON_ID);
        Transaction voided = aTransaction(a2, manager);
        voided.setStatus(TransactionStatus.VOIDED);

        when(transactionRepository.findBySalonIdAndCreatedAtBetween(eq(SALON_ID), any(), any()))
                .thenReturn(List.of(completed, voided));
        when(appointmentRepository.findBySalonIdAndStartTimeBetween(eq(SALON_ID), any(), any()))
                .thenReturn(List.of());
        when(utilisateurRepository.findBySalonIdAndTypeAndActive(SALON_ID, Barber.class, true))
                .thenReturn(List.of());

        DashboardStatsResponse stats = dashboardService.getStats();

        // Only COMPLETED transactions count
        assertThat(stats.getRevenueToday()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void getStats_appointmentsByStatus() {
        Appointment scheduled = anAppointment(AppointmentStatus.SCHEDULED, SALON_ID);
        Appointment completed = anAppointment(AppointmentStatus.COMPLETED, SALON_ID);

        when(transactionRepository.findBySalonIdAndCreatedAtBetween(eq(SALON_ID), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.findBySalonIdAndStartTimeBetween(eq(SALON_ID), any(), any()))
                .thenReturn(List.of(scheduled, completed));
        when(utilisateurRepository.findBySalonIdAndTypeAndActive(SALON_ID, Barber.class, true))
                .thenReturn(List.of());

        DashboardStatsResponse stats = dashboardService.getStats();

        assertThat(stats.getAppointmentsToday()).isEqualTo(2);
        assertThat(stats.getAppointmentsByStatus())
                .containsEntry("SCHEDULED", 1)
                .containsEntry("COMPLETED", 1)
                .containsEntry("IN_PROGRESS", 0)
                .containsEntry("CANCELLED", 0)
                .containsEntry("NO_SHOW", 0);
    }

    @Test
    void getRevenue_includesZeroDays() {
        // 3-day range, but only day 2 has transactions
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 3);

        Appointment a = anAppointment(AppointmentStatus.COMPLETED, SALON_ID);
        Transaction tx = aTransaction(a, manager);
        tx.setCreatedAt(LocalDate.of(2026, 3, 2).atTime(14, 0));

        when(transactionRepository.findBySalonIdAndCreatedAtBetween(eq(SALON_ID), any(), any()))
                .thenReturn(List.of(tx));

        List<DailyRevenueResponse> result = dashboardService.getRevenue(start, end);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getRevenue()).isEqualByComparingTo(BigDecimal.ZERO); // March 1
        assertThat(result.get(1).getRevenue()).isEqualByComparingTo(new BigDecimal("1500.00")); // March 2
        assertThat(result.get(2).getRevenue()).isEqualByComparingTo(BigDecimal.ZERO); // March 3
    }

    @Test
    void getRevenue_dateRange() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 1);

        when(transactionRepository.findBySalonIdAndCreatedAtBetween(eq(SALON_ID), any(), any()))
                .thenReturn(List.of());

        List<DailyRevenueResponse> result = dashboardService.getRevenue(start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo("2026-03-01");
        assertThat(result.get(0).getTransactionCount()).isZero();
    }
}
