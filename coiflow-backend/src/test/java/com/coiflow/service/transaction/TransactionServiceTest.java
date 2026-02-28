package com.coiflow.service.transaction;

import com.coiflow.TestSecurityUtils;
import com.coiflow.dto.transaction.CreateTransactionRequest;
import com.coiflow.dto.transaction.PaymentLine;
import com.coiflow.dto.transaction.TransactionResponse;
import com.coiflow.exception.BusinessException;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.appointment.Appointment;
import com.coiflow.model.commission.Commission;
import com.coiflow.model.enums.AppointmentStatus;
import com.coiflow.model.enums.PaymentMethod;
import com.coiflow.model.enums.TransactionStatus;
import com.coiflow.model.transaction.Transaction;
import com.coiflow.model.user.Barber;
import com.coiflow.model.user.Manager;
import com.coiflow.repository.appointment.AppointmentRepository;
import com.coiflow.repository.commission.CommissionRepository;
import com.coiflow.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static com.coiflow.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private CommissionRepository commissionRepository;

    @InjectMocks private TransactionService transactionService;

    private Manager manager;
    private Appointment completedAppointment;

    @BeforeEach
    void setUp() {
        TestSecurityUtils.setTenantContext(SALON_ID);
        manager = aManager(SALON_ID);
        TestSecurityUtils.mockSecurityContext(manager);
        completedAppointment = anAppointment(AppointmentStatus.COMPLETED, SALON_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAll();
    }

    // ── Helpers ────────────────────────────────────────────

    private CreateTransactionRequest buildRequest(BigDecimal... amounts) {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setAppointmentId(completedAppointment.getId());
        List<PaymentLine> payments = new java.util.ArrayList<>();
        for (BigDecimal amount : amounts) {
            PaymentLine pl = new PaymentLine();
            pl.setMethod(PaymentMethod.CASH);
            pl.setAmount(amount);
            payments.add(pl);
        }
        req.setPayments(payments);
        return req;
    }

    private void stubHappyPath() {
        when(appointmentRepository.findByIdAndSalonId(completedAppointment.getId(), SALON_ID))
                .thenReturn(Optional.of(completedAppointment));
        when(transactionRepository.existsByAppointmentId(completedAppointment.getId()))
                .thenReturn(false);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(commissionRepository.save(any(Commission.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── create: success ────────────────────────────────────

    @Test
    void create_success() {
        stubHappyPath();
        CreateTransactionRequest req = buildRequest(new BigDecimal("1500.00"));

        TransactionResponse response = transactionService.create(req);

        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getPayments()).hasSize(1);
        verify(transactionRepository).save(any(Transaction.class));
        verify(commissionRepository).save(any(Commission.class));
    }

    @Test
    void create_multiplePayments() {
        stubHappyPath();
        // Split payment: 1000 CASH + 500 CASH = 1500 total
        CreateTransactionRequest req = buildRequest(new BigDecimal("1000.00"), new BigDecimal("500.00"));

        TransactionResponse response = transactionService.create(req);

        assertThat(response.getPayments()).hasSize(2);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    // ── create: commission calculation ─────────────────────

    @Test
    void create_commissionCalculation() {
        stubHappyPath();
        // Barber has 30% commission rate by default from aBarber()
        CreateTransactionRequest req = buildRequest(new BigDecimal("1500.00"));

        transactionService.create(req);

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionRepository).save(captor.capture());
        Commission commission = captor.getValue();

        // 1500 × 30 / 100 = 450.00
        assertThat(commission.getAmount()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(commission.getRateApplied()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void create_commissionRounding() {
        // Use a rate that causes rounding: 33.33%
        Barber barber = (Barber) completedAppointment.getBarber();
        barber.setCommissionRate(new BigDecimal("33.33"));
        stubHappyPath();

        CreateTransactionRequest req = buildRequest(new BigDecimal("1500.00"));
        transactionService.create(req);

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionRepository).save(captor.capture());

        // 1500 × 33.33 / 100 = 499.95 (HALF_UP)
        BigDecimal expected = new BigDecimal("1500.00")
                .multiply(new BigDecimal("33.33"))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(expected);
    }

    @Test
    void create_barberNoCommissionRate() {
        Barber barber = (Barber) completedAppointment.getBarber();
        barber.setCommissionRate(null);
        stubHappyPath();

        CreateTransactionRequest req = buildRequest(new BigDecimal("1500.00"));
        transactionService.create(req);

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── create: validation errors ──────────────────────────

    @Test
    void create_appointmentNotCompleted_throws() {
        Appointment scheduled = anAppointment(AppointmentStatus.SCHEDULED, SALON_ID);
        when(appointmentRepository.findByIdAndSalonId(scheduled.getId(), SALON_ID))
                .thenReturn(Optional.of(scheduled));

        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setAppointmentId(scheduled.getId());
        req.setPayments(List.of());

        assertThatThrownBy(() -> transactionService.create(req))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_STATUS");
    }

    @Test
    void create_alreadyCashed_throws() {
        when(appointmentRepository.findByIdAndSalonId(completedAppointment.getId(), SALON_ID))
                .thenReturn(Optional.of(completedAppointment));
        when(transactionRepository.existsByAppointmentId(completedAppointment.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> transactionService.create(buildRequest(new BigDecimal("1500.00"))))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ALREADY_CASHED");
    }

    @Test
    void create_paymentMismatch_over_throws() {
        when(appointmentRepository.findByIdAndSalonId(completedAppointment.getId(), SALON_ID))
                .thenReturn(Optional.of(completedAppointment));
        when(transactionRepository.existsByAppointmentId(completedAppointment.getId()))
                .thenReturn(false);

        // Total is 1500, but payment is 2000
        assertThatThrownBy(() -> transactionService.create(buildRequest(new BigDecimal("2000.00"))))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PAYMENT_MISMATCH");
    }

    @Test
    void create_paymentMismatch_under_throws() {
        when(appointmentRepository.findByIdAndSalonId(completedAppointment.getId(), SALON_ID))
                .thenReturn(Optional.of(completedAppointment));
        when(transactionRepository.existsByAppointmentId(completedAppointment.getId()))
                .thenReturn(false);

        // Total is 1500, but payment is 1000
        assertThatThrownBy(() -> transactionService.create(buildRequest(new BigDecimal("1000.00"))))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PAYMENT_MISMATCH");
    }

    @Test
    void create_appointmentNotFound_throws() {
        when(appointmentRepository.findByIdAndSalonId("unknown", SALON_ID))
                .thenReturn(Optional.empty());

        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setAppointmentId("unknown");
        req.setPayments(List.of());

        assertThatThrownBy(() -> transactionService.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_missingSecurityContext_throws() {
        TestSecurityUtils.clearAll();
        TestSecurityUtils.setTenantContext(SALON_ID);
        // SecurityContextHolder has no authentication → NPE on getPrincipal()

        assertThatThrownBy(() -> transactionService.create(buildRequest(new BigDecimal("1500.00"))))
                .isInstanceOf(NullPointerException.class);
    }

    // ── voidTransaction ────────────────────────────────────

    @Test
    void voidTransaction_success() {
        Transaction tx = aTransaction(completedAppointment, manager);
        when(transactionRepository.findByIdAndSalonId(tx.getId(), SALON_ID))
                .thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(commissionRepository.findByTransactionId(tx.getId()))
                .thenReturn(Optional.of(aCommission(tx, new BigDecimal("30.00"))));

        TransactionResponse response = transactionService.voidTransaction(tx.getId());

        assertThat(response.getStatus()).isEqualTo("VOIDED");
        verify(transactionRepository).save(any());
    }

    @Test
    void voidTransaction_alreadyVoided_noop() {
        Transaction tx = aTransaction(completedAppointment, manager);
        tx.setStatus(TransactionStatus.VOIDED);
        when(transactionRepository.findByIdAndSalonId(tx.getId(), SALON_ID))
                .thenReturn(Optional.of(tx));
        when(commissionRepository.findByTransactionId(tx.getId()))
                .thenReturn(Optional.empty());

        TransactionResponse response = transactionService.voidTransaction(tx.getId());

        assertThat(response.getStatus()).isEqualTo("VOIDED");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void voidTransaction_commissionPreserved() {
        Transaction tx = aTransaction(completedAppointment, manager);
        Commission commission = aCommission(tx, new BigDecimal("30.00"));
        when(transactionRepository.findByIdAndSalonId(tx.getId(), SALON_ID))
                .thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(commissionRepository.findByTransactionId(tx.getId()))
                .thenReturn(Optional.of(commission));

        transactionService.voidTransaction(tx.getId());

        // Commission is NOT deleted — audit trail
        verify(commissionRepository, never()).delete(any());
        verify(commissionRepository, never()).deleteById(any());
    }

    @Test
    void voidTransaction_notFound_throws() {
        when(transactionRepository.findByIdAndSalonId("unknown", SALON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.voidTransaction("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getById ────────────────────────────────────────────

    @Test
    void getById_notFound_throws() {
        when(transactionRepository.findByIdAndSalonId("unknown", SALON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
