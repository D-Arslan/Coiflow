package com.coiflow.service.commission;

import com.coiflow.TestSecurityUtils;
import com.coiflow.dto.commission.CommissionResponse;
import com.coiflow.model.commission.Commission;
import com.coiflow.model.enums.AppointmentStatus;
import com.coiflow.model.transaction.Transaction;
import com.coiflow.model.user.Barber;
import com.coiflow.model.user.Manager;
import com.coiflow.repository.commission.CommissionRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock private CommissionRepository commissionRepository;

    @InjectMocks private CommissionService commissionService;

    private Manager manager;
    private Barber barber;

    @BeforeEach
    void setUp() {
        TestSecurityUtils.setTenantContext(SALON_ID);
        manager = aManager(SALON_ID);
        barber = aBarber(SALON_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAll();
    }

    private Commission buildCommission() {
        var appointment = anAppointment(AppointmentStatus.COMPLETED, SALON_ID);
        Transaction tx = aTransaction(appointment, manager);
        return aCommission(tx, new BigDecimal("30.00"));
    }

    @Test
    void getAll_asManager() {
        TestSecurityUtils.mockSecurityContext(manager);
        Commission c = buildCommission();
        when(commissionRepository.findBySalonIdAndCreatedAtBetween(eq(SALON_ID), any(), any()))
                .thenReturn(List.of(c));

        List<CommissionResponse> result = commissionService.getAll(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);

        assertThat(result).hasSize(1);
        verify(commissionRepository).findBySalonIdAndCreatedAtBetween(eq(SALON_ID), any(), any());
    }

    @Test
    void getAll_asManager_withBarberFilter() {
        TestSecurityUtils.mockSecurityContext(manager);
        Commission c = buildCommission();
        when(commissionRepository.findByBarberIdAndCreatedAtBetween(eq(barber.getId()), any(), any()))
                .thenReturn(List.of(c));

        List<CommissionResponse> result = commissionService.getAll(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), barber.getId());

        assertThat(result).hasSize(1);
        verify(commissionRepository).findByBarberIdAndCreatedAtBetween(eq(barber.getId()), any(), any());
    }

    @Test
    void getAll_asBarber_forcedToOwnId() {
        TestSecurityUtils.mockSecurityContext(barber);
        when(commissionRepository.findByBarberIdAndCreatedAtBetween(eq(barber.getId()), any(), any()))
                .thenReturn(List.of());

        commissionService.getAll(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);

        // Even though barberId was null, should force to barber's own ID
        verify(commissionRepository).findByBarberIdAndCreatedAtBetween(eq(barber.getId()), any(), any());
    }

    @Test
    void getAll_asBarber_otherBarberIdIgnored() {
        TestSecurityUtils.mockSecurityContext(barber);
        when(commissionRepository.findByBarberIdAndCreatedAtBetween(eq(barber.getId()), any(), any()))
                .thenReturn(List.of());

        // Pass a different barber's ID — should be ignored and forced to own
        commissionService.getAll(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), "other-barber-id");

        verify(commissionRepository).findByBarberIdAndCreatedAtBetween(eq(barber.getId()), any(), any());
        verify(commissionRepository, never()).findByBarberIdAndCreatedAtBetween(eq("other-barber-id"), any(), any());
    }

    @Test
    void getAll_missingContext_throws() {
        TestSecurityUtils.clearAll();
        TestSecurityUtils.mockSecurityContext(manager);

        assertThatThrownBy(() -> commissionService.getAll(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("salon manquant");
    }

    @Test
    void getAll_missingSecurityContext_throws() {
        // TenantContext set, but no SecurityContext → NPE on getPrincipal()
        assertThatThrownBy(() -> commissionService.getAll(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null))
                .isInstanceOf(NullPointerException.class);
    }
}
