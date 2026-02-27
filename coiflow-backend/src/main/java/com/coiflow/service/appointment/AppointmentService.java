package com.coiflow.service.appointment;

import com.coiflow.dto.appointment.*;
import com.coiflow.exception.BusinessException;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.appointment.Appointment;
import com.coiflow.model.appointment.AppointmentServiceItem;
import com.coiflow.model.catalog.ServiceItem;
import com.coiflow.model.client.Client;
import com.coiflow.model.enums.AppointmentStatus;
import com.coiflow.model.user.Barber;
import com.coiflow.model.user.Utilisateur;
import com.coiflow.repository.appointment.AppointmentRepository;
import com.coiflow.repository.catalog.ServiceItemRepository;
import com.coiflow.repository.client.ClientRepository;
import com.coiflow.repository.user.UtilisateurRepository;
import com.coiflow.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ClientRepository clientRepository;
    private final ServiceItemRepository serviceItemRepository;

    private String requireSalonId() {
        String salonId = TenantContextHolder.getSalonId();
        if (salonId == null || salonId.isBlank()) {
            throw new IllegalStateException("Contexte de salon manquant");
        }
        return salonId;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','BARBER')")
    public AppointmentResponse create(CreateAppointmentRequest request) {
        String salonId = requireSalonId();

        // Load & verify barber belongs to salon
        Utilisateur barberUser = utilisateurRepository.findByIdAndType(request.getBarberId(), Barber.class)
                .filter(b -> salonId.equals(b.getSalonId()))
                .orElseThrow(() -> new ResourceNotFoundException("Coiffeur introuvable"));

        // Load & verify client if provided
        Client client = null;
        if (request.getClientId() != null && !request.getClientId().isBlank()) {
            client = clientRepository.findByIdAndSalon_Id(request.getClientId(), salonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
        }

        // Load services & verify they belong to salon and are active
        List<ServiceItem> serviceItems = request.getServiceIds().stream()
                .map(serviceId -> serviceItemRepository.findByIdAndSalon_Id(serviceId, salonId)
                        .filter(ServiceItem::isActive)
                        .orElseThrow(() -> new ResourceNotFoundException("Prestation introuvable: " + serviceId)))
                .toList();

        // Calculate endTime
        int totalDuration = serviceItems.stream()
                .mapToInt(ServiceItem::getDurationMinutes)
                .sum();
        if (totalDuration <= 0) {
            throw new IllegalArgumentException("La duree totale doit etre superieure a 0");
        }
        LocalDateTime endTime = request.getStartTime().plusMinutes(totalDuration);

        // Anti double-booking with pessimistic lock
        List<Appointment> overlapping = appointmentRepository.findOverlappingForUpdate(
                request.getBarberId(), request.getStartTime(), endTime);
        if (!overlapping.isEmpty()) {
            throw new BusinessException("APPOINTMENT_OVERLAP", "Ce creneau est deja occupe pour ce coiffeur");
        }

        // Create appointment
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID().toString())
                .salon(barberUser.getSalon())
                .barber(barberUser)
                .client(client)
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.getNotes())
                .build();

        // Create service line items with price snapshot
        List<AppointmentServiceItem> items = serviceItems.stream()
                .map(si -> AppointmentServiceItem.builder()
                        .id(UUID.randomUUID().toString())
                        .appointment(appointment)
                        .service(si)
                        .priceApplied(si.getPrice())
                        .build())
                .toList();
        appointment.getServices().addAll(items);

        appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @PreAuthorize("hasAnyRole('MANAGER','BARBER')")
    public List<AppointmentResponse> getByDateRange(LocalDate start, LocalDate end, String barberId) {
        String salonId = requireSalonId();
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay();

        List<Appointment> appointments;
        if (barberId != null && !barberId.isBlank()) {
            appointments = appointmentRepository.findBySalonIdAndBarberIdAndStartTimeBetween(
                    salonId, barberId, startDt, endDt);
        } else {
            appointments = appointmentRepository.findBySalonIdAndStartTimeBetween(salonId, startDt, endDt);
        }
        return appointments.stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('MANAGER','BARBER')")
    public List<AppointmentResponse> getToCash(LocalDate start, LocalDate end) {
        String salonId = requireSalonId();
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay();
        return appointmentRepository.findToCash(salonId, startDt, endDt)
                .stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('MANAGER','BARBER')")
    public AppointmentResponse getById(String id) {
        String salonId = requireSalonId();
        Appointment a = appointmentRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable"));
        return toResponse(a);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','BARBER')")
    public AppointmentResponse updateStatus(String id, AppointmentStatus newStatus) {
        String salonId = requireSalonId();
        Appointment a = appointmentRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable"));

        AppointmentStatus current = a.getStatus();

        // Idempotent: same status = no-op
        if (current == newStatus) {
            return toResponse(a);
        }

        // Terminal states: no transition allowed
        Set<AppointmentStatus> terminal = Set.of(
                AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW);
        if (terminal.contains(current)) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "Impossible de changer le statut d'un rendez-vous " + current.name().toLowerCase());
        }

        // Valid transitions
        boolean valid = switch (current) {
            case SCHEDULED -> newStatus == AppointmentStatus.IN_PROGRESS
                    || newStatus == AppointmentStatus.CANCELLED
                    || newStatus == AppointmentStatus.NO_SHOW;
            case IN_PROGRESS -> newStatus == AppointmentStatus.COMPLETED
                    || newStatus == AppointmentStatus.CANCELLED;
            default -> false;
        };

        if (!valid) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "Transition invalide de " + current + " vers " + newStatus);
        }

        a.setStatus(newStatus);
        return toResponse(appointmentRepository.save(a));
    }

    private AppointmentResponse toResponse(Appointment a) {
        List<ServiceLineResponse> serviceLines = a.getServices().stream()
                .map(si -> ServiceLineResponse.builder()
                        .serviceId(si.getService().getId())
                        .serviceName(si.getService().getName())
                        .priceApplied(si.getPriceApplied())
                        .durationMinutes(si.getService().getDurationMinutes())
                        .build())
                .toList();

        BigDecimal totalPrice = a.getServices().stream()
                .map(AppointmentServiceItem::getPriceApplied)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AppointmentResponse.builder()
                .id(a.getId())
                .barberId(a.getBarber().getId())
                .barberName(a.getBarber().getFirstName() + " " + a.getBarber().getLastName())
                .clientId(a.getClient() != null ? a.getClient().getId() : null)
                .clientName(a.getClient() != null ? a.getClient().getLastName() + " " + a.getClient().getFirstName() : null)
                .startTime(a.getStartTime().toString())
                .endTime(a.getEndTime().toString())
                .status(a.getStatus().name())
                .notes(a.getNotes())
                .services(serviceLines)
                .totalPrice(totalPrice)
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                .build();
    }
}
