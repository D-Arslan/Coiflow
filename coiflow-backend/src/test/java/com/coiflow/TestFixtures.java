package com.coiflow;

import com.coiflow.model.appointment.Appointment;
import com.coiflow.model.appointment.AppointmentServiceItem;
import com.coiflow.model.catalog.ServiceItem;
import com.coiflow.model.client.Client;
import com.coiflow.model.commission.Commission;
import com.coiflow.model.enums.AppointmentStatus;
import com.coiflow.model.enums.PaymentMethod;
import com.coiflow.model.enums.TransactionStatus;
import com.coiflow.model.salon.Salon;
import com.coiflow.model.transaction.Payment;
import com.coiflow.model.transaction.Transaction;
import com.coiflow.model.user.Barber;
import com.coiflow.model.user.Manager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Shared test fixtures — reduces noise in unit tests.
 * All builders produce detached entities (no Spring context required).
 */
public final class TestFixtures {

    public static final String SALON_ID = "salon-001";
    public static final String OTHER_SALON_ID = "salon-999";

    private TestFixtures() {}

    // ── Salon ──────────────────────────────────────────────

    public static Salon aSalon() {
        return aSalon(SALON_ID);
    }

    public static Salon aSalon(String id) {
        return Salon.builder()
                .id(id)
                .name("Salon Test")
                .address("123 Rue Test")
                .phone("0555000000")
                .email("salon@test.com")
                .active(true)
                .build();
    }

    // ── Barber ─────────────────────────────────────────────

    public static Barber aBarber(String salonId) {
        return aBarber(salonId, new BigDecimal("30.00"));
    }

    public static Barber aBarber(String salonId, BigDecimal commissionRate) {
        Salon salon = aSalon(salonId);
        Barber barber = new Barber();
        barber.setId(UUID.randomUUID().toString());
        barber.setSalon(salon);
        barber.setFirstName("Ali");
        barber.setLastName("Coiffeur");
        barber.setEmail("ali@test.com");
        barber.setPasswordHash("hashed");
        barber.setActive(true);
        barber.setCommissionRate(commissionRate);
        return barber;
    }

    // ── Manager ────────────────────────────────────────────

    public static Manager aManager(String salonId) {
        Salon salon = aSalon(salonId);
        Manager manager = new Manager();
        manager.setId(UUID.randomUUID().toString());
        manager.setSalon(salon);
        manager.setFirstName("Karim");
        manager.setLastName("Gerant");
        manager.setEmail("karim@test.com");
        manager.setPasswordHash("hashed");
        manager.setActive(true);
        return manager;
    }

    // ── Client ─────────────────────────────────────────────

    public static Client aClient(String salonId) {
        return Client.builder()
                .id(UUID.randomUUID().toString())
                .salon(aSalon(salonId))
                .firstName("Mohamed")
                .lastName("Client")
                .phone("0666000000")
                .email("mohamed@test.com")
                .build();
    }

    // ── ServiceItem ────────────────────────────────────────

    public static ServiceItem aServiceItem(BigDecimal price, int durationMinutes) {
        return aServiceItem(SALON_ID, price, durationMinutes);
    }

    public static ServiceItem aServiceItem(String salonId, BigDecimal price, int durationMinutes) {
        return ServiceItem.builder()
                .id(UUID.randomUUID().toString())
                .salon(aSalon(salonId))
                .name("Coupe")
                .durationMinutes(durationMinutes)
                .price(price)
                .active(true)
                .build();
    }

    // ── Appointment ────────────────────────────────────────

    public static Appointment anAppointment(AppointmentStatus status, String salonId) {
        Barber barber = aBarber(salonId);
        Client client = aClient(salonId);
        Salon salon = aSalon(salonId);
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 10, 0);

        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID().toString())
                .salon(salon)
                .barber(barber)
                .client(client)
                .startTime(start)
                .endTime(start.plusMinutes(30))
                .status(status)
                .services(new ArrayList<>())
                .build();

        ServiceItem si = aServiceItem(salonId, new BigDecimal("1500.00"), 30);
        AppointmentServiceItem line = AppointmentServiceItem.builder()
                .id(UUID.randomUUID().toString())
                .appointment(appointment)
                .service(si)
                .priceApplied(si.getPrice())
                .build();
        appointment.getServices().add(line);

        return appointment;
    }

    // ── Transaction ────────────────────────────────────────

    public static Transaction aTransaction(Appointment appointment, Manager createdBy) {
        BigDecimal total = appointment.getServices().stream()
                .map(AppointmentServiceItem::getPriceApplied)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .salon(appointment.getSalon())
                .appointment(appointment)
                .barber(appointment.getBarber())
                .totalAmount(total)
                .status(TransactionStatus.COMPLETED)
                .createdBy(createdBy)
                .payments(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .transaction(tx)
                .method(PaymentMethod.CASH)
                .amount(total)
                .build();
        tx.getPayments().add(payment);

        return tx;
    }

    // ── Commission ─────────────────────────────────────────

    public static Commission aCommission(Transaction tx, BigDecimal rate) {
        BigDecimal amount = tx.getTotalAmount()
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        return Commission.builder()
                .id(UUID.randomUUID().toString())
                .salon(tx.getSalon())
                .barber(tx.getBarber())
                .transaction(tx)
                .rateApplied(rate)
                .amount(amount)
                .periodStart(tx.getCreatedAt().toLocalDate())
                .periodEnd(tx.getCreatedAt().toLocalDate())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
