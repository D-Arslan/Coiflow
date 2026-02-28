package com.coiflow.service.appointment;

import com.coiflow.TestSecurityUtils;
import com.coiflow.dto.appointment.AppointmentResponse;
import com.coiflow.dto.appointment.CreateAppointmentRequest;
import com.coiflow.exception.BusinessException;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.appointment.Appointment;
import com.coiflow.model.catalog.ServiceItem;
import com.coiflow.model.client.Client;
import com.coiflow.model.enums.AppointmentStatus;
import com.coiflow.model.user.Barber;
import com.coiflow.repository.appointment.AppointmentRepository;
import com.coiflow.repository.catalog.ServiceItemRepository;
import com.coiflow.repository.client.ClientRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.coiflow.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ServiceItemRepository serviceItemRepository;

    @InjectMocks private AppointmentService appointmentService;

    private Barber barber;
    private Client client;
    private ServiceItem serviceItem;

    @BeforeEach
    void setUp() {
        TestSecurityUtils.setTenantContext(SALON_ID);
        barber = aBarber(SALON_ID);
        client = aClient(SALON_ID);
        serviceItem = aServiceItem(new BigDecimal("1500.00"), 30);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAll();
    }

    // ── Helpers ────────────────────────────────────────────

    private CreateAppointmentRequest buildRequest() {
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setBarberId(barber.getId());
        req.setClientId(client.getId());
        req.setStartTime(LocalDateTime.of(2026, 3, 1, 10, 0));
        req.setServiceIds(List.of(serviceItem.getId()));
        req.setNotes("Test note");
        return req;
    }

    private void stubHappyPath() {
        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.of(barber));
        when(clientRepository.findByIdAndSalon_Id(client.getId(), SALON_ID))
                .thenReturn(Optional.of(client));
        when(serviceItemRepository.findByIdAndSalon_Id(serviceItem.getId(), SALON_ID))
                .thenReturn(Optional.of(serviceItem));
        when(appointmentRepository.findOverlappingForUpdate(anyString(), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── create: success ────────────────────────────────────

    @Test
    void create_success() {
        stubHappyPath();
        CreateAppointmentRequest req = buildRequest();

        AppointmentResponse response = appointmentService.create(req);

        assertThat(response.getBarberId()).isEqualTo(barber.getId());
        assertThat(response.getClientId()).isEqualTo(client.getId());
        assertThat(response.getStatus()).isEqualTo("SCHEDULED");
        assertThat(response.getServices()).hasSize(1);
        assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1500.00"));
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void create_withoutClient_success() {
        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.of(barber));
        when(serviceItemRepository.findByIdAndSalon_Id(serviceItem.getId(), SALON_ID))
                .thenReturn(Optional.of(serviceItem));
        when(appointmentRepository.findOverlappingForUpdate(anyString(), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateAppointmentRequest req = buildRequest();
        req.setClientId(null);

        AppointmentResponse response = appointmentService.create(req);

        assertThat(response.getClientId()).isNull();
        assertThat(response.getClientName()).isNull();
    }

    @Test
    void create_priceSnapshot() {
        stubHappyPath();

        AppointmentResponse response = appointmentService.create(buildRequest());

        assertThat(response.getServices().get(0).getPriceApplied())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    // ── create: barber errors ──────────────────────────────

    @Test
    void create_barberNotFound_throws() {
        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.create(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_barberWrongSalon_throws() {
        Barber otherBarber = aBarber(OTHER_SALON_ID);
        otherBarber.setId(barber.getId());

        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.of(otherBarber));

        assertThatThrownBy(() -> appointmentService.create(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create: service errors ─────────────────────────────

    @Test
    void create_serviceNotFound_throws() {
        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.of(barber));
        when(clientRepository.findByIdAndSalon_Id(client.getId(), SALON_ID))
                .thenReturn(Optional.of(client));
        when(serviceItemRepository.findByIdAndSalon_Id(serviceItem.getId(), SALON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.create(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_serviceInactive_throws() {
        serviceItem.setActive(false);

        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.of(barber));
        when(clientRepository.findByIdAndSalon_Id(client.getId(), SALON_ID))
                .thenReturn(Optional.of(client));
        when(serviceItemRepository.findByIdAndSalon_Id(serviceItem.getId(), SALON_ID))
                .thenReturn(Optional.of(serviceItem));

        assertThatThrownBy(() -> appointmentService.create(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_serviceFromOtherSalon_throws() {
        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.of(barber));
        when(clientRepository.findByIdAndSalon_Id(client.getId(), SALON_ID))
                .thenReturn(Optional.of(client));
        // Service exists but not in this salon → repo returns empty
        when(serviceItemRepository.findByIdAndSalon_Id(serviceItem.getId(), SALON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.create(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create: overlap ────────────────────────────────────

    @Test
    void create_overlap_throws() {
        Appointment existing = anAppointment(AppointmentStatus.SCHEDULED, SALON_ID);

        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.of(barber));
        when(clientRepository.findByIdAndSalon_Id(client.getId(), SALON_ID))
                .thenReturn(Optional.of(client));
        when(serviceItemRepository.findByIdAndSalon_Id(serviceItem.getId(), SALON_ID))
                .thenReturn(Optional.of(serviceItem));
        when(appointmentRepository.findOverlappingForUpdate(anyString(), any(), any()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> appointmentService.create(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("APPOINTMENT_OVERLAP");
    }

    // ── create: zero duration ──────────────────────────────

    @Test
    void create_zeroDuration_throws() {
        ServiceItem zeroDuration = aServiceItem(new BigDecimal("500.00"), 0);

        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.of(barber));
        when(clientRepository.findByIdAndSalon_Id(client.getId(), SALON_ID))
                .thenReturn(Optional.of(client));
        when(serviceItemRepository.findByIdAndSalon_Id(anyString(), eq(SALON_ID)))
                .thenReturn(Optional.of(zeroDuration));

        CreateAppointmentRequest req = buildRequest();
        req.setServiceIds(List.of(zeroDuration.getId()));

        assertThatThrownBy(() -> appointmentService.create(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── updateStatus: valid transitions ────────────────────

    @Test
    void updateStatus_scheduledToInProgress() {
        Appointment a = anAppointment(AppointmentStatus.SCHEDULED, SALON_ID);
        when(appointmentRepository.findByIdAndSalonId(a.getId(), SALON_ID))
                .thenReturn(Optional.of(a));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.updateStatus(a.getId(), AppointmentStatus.IN_PROGRESS);

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void updateStatus_scheduledToCancelled() {
        Appointment a = anAppointment(AppointmentStatus.SCHEDULED, SALON_ID);
        when(appointmentRepository.findByIdAndSalonId(a.getId(), SALON_ID))
                .thenReturn(Optional.of(a));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.updateStatus(a.getId(), AppointmentStatus.CANCELLED);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void updateStatus_scheduledToNoShow() {
        Appointment a = anAppointment(AppointmentStatus.SCHEDULED, SALON_ID);
        when(appointmentRepository.findByIdAndSalonId(a.getId(), SALON_ID))
                .thenReturn(Optional.of(a));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.updateStatus(a.getId(), AppointmentStatus.NO_SHOW);

        assertThat(response.getStatus()).isEqualTo("NO_SHOW");
    }

    @Test
    void updateStatus_inProgressToCompleted() {
        Appointment a = anAppointment(AppointmentStatus.IN_PROGRESS, SALON_ID);
        when(appointmentRepository.findByIdAndSalonId(a.getId(), SALON_ID))
                .thenReturn(Optional.of(a));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.updateStatus(a.getId(), AppointmentStatus.COMPLETED);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    // ── updateStatus: terminal state ───────────────────────

    @Test
    void updateStatus_completedToAnything_throws() {
        Appointment a = anAppointment(AppointmentStatus.COMPLETED, SALON_ID);
        when(appointmentRepository.findByIdAndSalonId(a.getId(), SALON_ID))
                .thenReturn(Optional.of(a));

        assertThatThrownBy(() -> appointmentService.updateStatus(a.getId(), AppointmentStatus.IN_PROGRESS))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    // ── updateStatus: invalid transition ───────────────────

    @Test
    void updateStatus_invalidTransition_throws() {
        // SCHEDULED → COMPLETED is not valid (must go through IN_PROGRESS)
        Appointment a = anAppointment(AppointmentStatus.SCHEDULED, SALON_ID);
        when(appointmentRepository.findByIdAndSalonId(a.getId(), SALON_ID))
                .thenReturn(Optional.of(a));

        assertThatThrownBy(() -> appointmentService.updateStatus(a.getId(), AppointmentStatus.COMPLETED))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    // ── updateStatus: idempotent ───────────────────────────

    @Test
    void updateStatus_sameStatus_noop() {
        Appointment a = anAppointment(AppointmentStatus.SCHEDULED, SALON_ID);
        when(appointmentRepository.findByIdAndSalonId(a.getId(), SALON_ID))
                .thenReturn(Optional.of(a));

        AppointmentResponse response = appointmentService.updateStatus(a.getId(), AppointmentStatus.SCHEDULED);

        assertThat(response.getStatus()).isEqualTo("SCHEDULED");
        verify(appointmentRepository, never()).save(any());
    }

    // ── updateStatus: cross-tenant ─────────────────────────

    @Test
    void updateStatus_crossTenant_throws() {
        when(appointmentRepository.findByIdAndSalonId("rdv-xyz", SALON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.updateStatus("rdv-xyz", AppointmentStatus.IN_PROGRESS))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getByDateRange ─────────────────────────────────────

    @Test
    void getByDateRange_withBarberId_filtersCorrectly() {
        Appointment a = anAppointment(AppointmentStatus.SCHEDULED, SALON_ID);
        when(appointmentRepository.findBySalonIdAndBarberIdAndStartTimeBetween(
                eq(SALON_ID), eq(barber.getId()), any(), any()))
                .thenReturn(List.of(a));

        List<AppointmentResponse> result = appointmentService.getByDateRange(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), barber.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void getByDateRange_withoutBarberId_returnsSalonWide() {
        when(appointmentRepository.findBySalonIdAndStartTimeBetween(
                eq(SALON_ID), any(), any()))
                .thenReturn(List.of());

        List<AppointmentResponse> result = appointmentService.getByDateRange(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), null);

        assertThat(result).isEmpty();
        verify(appointmentRepository).findBySalonIdAndStartTimeBetween(eq(SALON_ID), any(), any());
    }
}
