package com.coiflow.service.transaction;

import com.coiflow.dto.transaction.CreateTransactionRequest;
import com.coiflow.dto.transaction.TransactionResponse;
import com.coiflow.exception.BusinessException;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.appointment.Appointment;
import com.coiflow.model.appointment.AppointmentServiceItem;
import com.coiflow.model.commission.Commission;
import com.coiflow.model.enums.AppointmentStatus;
import com.coiflow.model.enums.TransactionStatus;
import com.coiflow.model.transaction.Payment;
import com.coiflow.model.transaction.Transaction;
import com.coiflow.model.user.Barber;
import com.coiflow.model.user.Utilisateur;
import com.coiflow.repository.appointment.AppointmentRepository;
import com.coiflow.repository.commission.CommissionRepository;
import com.coiflow.repository.transaction.TransactionRepository;
import com.coiflow.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AppointmentRepository appointmentRepository;
    private final CommissionRepository commissionRepository;

    private String requireSalonId() {
        String salonId = TenantContextHolder.getSalonId();
        if (salonId == null || salonId.isBlank()) {
            throw new IllegalStateException("Contexte de salon manquant");
        }
        return salonId;
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public TransactionResponse create(CreateTransactionRequest request) {
        String salonId = requireSalonId();
        Utilisateur currentUser = (Utilisateur) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Load appointment & verify
        Appointment appointment = appointmentRepository.findByIdAndSalonId(request.getAppointmentId(), salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable"));

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessException("INVALID_STATUS", "Le rendez-vous doit etre termine pour etre encaisse");
        }

        // Check no existing transaction
        if (transactionRepository.existsByAppointmentId(appointment.getId())) {
            throw new BusinessException("ALREADY_CASHED", "Ce rendez-vous a deja ete encaisse");
        }

        // Calculate total from appointment services
        BigDecimal totalAmount = appointment.getServices().stream()
                .map(AppointmentServiceItem::getPriceApplied)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Validate payment sum
        BigDecimal paymentSum = request.getPayments().stream()
                .map(p -> p.getAmount().setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (paymentSum.compareTo(totalAmount) != 0) {
            throw new BusinessException("PAYMENT_MISMATCH",
                    "Le total des paiements (" + paymentSum + ") ne correspond pas au montant (" + totalAmount + ")");
        }

        // Create transaction
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .salon(appointment.getSalon())
                .appointment(appointment)
                .barber(appointment.getBarber())
                .totalAmount(totalAmount)
                .status(TransactionStatus.COMPLETED)
                .createdBy(currentUser)
                .build();

        // Create payments
        List<Payment> payments = request.getPayments().stream()
                .map(pl -> Payment.builder()
                        .id(UUID.randomUUID().toString())
                        .transaction(transaction)
                        .method(pl.getMethod())
                        .amount(pl.getAmount().setScale(2, RoundingMode.HALF_UP))
                        .build())
                .toList();
        transaction.getPayments().addAll(payments);

        transactionRepository.save(transaction);

        // Auto-create commission (unproxy to resolve Hibernate STI proxy)
        Barber barber = (Barber) Hibernate.unproxy(appointment.getBarber());
        BigDecimal commissionRate = barber.getCommissionRate() != null ? barber.getCommissionRate() : BigDecimal.ZERO;
        BigDecimal commissionAmount = totalAmount.multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Commission commission = Commission.builder()
                .id(UUID.randomUUID().toString())
                .salon(appointment.getSalon())
                .barber(barber)
                .transaction(transaction)
                .rateApplied(commissionRate)
                .amount(commissionAmount)
                .periodStart(appointment.getStartTime().toLocalDate())
                .periodEnd(appointment.getStartTime().toLocalDate())
                .build();
        commissionRepository.save(commission);

        return toResponse(transaction, commission);
    }

    @PreAuthorize("hasRole('MANAGER')")
    public List<TransactionResponse> getAll(LocalDate start, LocalDate end) {
        String salonId = requireSalonId();
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay();

        return transactionRepository.findBySalonIdAndCreatedAtBetween(salonId, startDt, endDt)
                .stream()
                .map(t -> {
                    Commission c = commissionRepository.findByTransactionId(t.getId()).orElse(null);
                    return toResponse(t, c);
                })
                .toList();
    }

    @PreAuthorize("hasRole('MANAGER')")
    public TransactionResponse getById(String id) {
        String salonId = requireSalonId();
        Transaction t = transactionRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction introuvable"));
        Commission c = commissionRepository.findByTransactionId(t.getId()).orElse(null);
        return toResponse(t, c);
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public TransactionResponse voidTransaction(String id) {
        String salonId = requireSalonId();
        Transaction t = transactionRepository.findByIdAndSalonId(id, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction introuvable"));

        if (t.getStatus() == TransactionStatus.VOIDED) {
            Commission c = commissionRepository.findByTransactionId(t.getId()).orElse(null);
            return toResponse(t, c);
        }

        t.setStatus(TransactionStatus.VOIDED);
        transactionRepository.save(t);

        // Commission is NOT voided — audit trail
        Commission c = commissionRepository.findByTransactionId(t.getId()).orElse(null);
        return toResponse(t, c);
    }

    private TransactionResponse toResponse(Transaction t, Commission c) {
        List<TransactionResponse.PaymentLineResponse> paymentLines = t.getPayments().stream()
                .map(p -> TransactionResponse.PaymentLineResponse.builder()
                        .method(p.getMethod().name())
                        .amount(p.getAmount())
                        .build())
                .toList();

        return TransactionResponse.builder()
                .id(t.getId())
                .appointmentId(t.getAppointment() != null ? t.getAppointment().getId() : null)
                .barberId(t.getBarber().getId())
                .barberName(t.getBarber().getFirstName() + " " + t.getBarber().getLastName())
                .totalAmount(t.getTotalAmount())
                .status(t.getStatus().name())
                .payments(paymentLines)
                .commissionRate(c != null ? c.getRateApplied() : null)
                .commissionAmount(c != null ? c.getAmount() : null)
                .createdBy(t.getCreatedBy().getFirstName() + " " + t.getCreatedBy().getLastName())
                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                .build();
    }
}
