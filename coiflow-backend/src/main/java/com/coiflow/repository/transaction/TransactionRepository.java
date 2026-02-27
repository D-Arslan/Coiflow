package com.coiflow.repository.transaction;

import com.coiflow.model.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findBySalonIdAndCreatedAtBetween(String salonId, LocalDateTime start, LocalDateTime end);

    Optional<Transaction> findByIdAndSalonId(String id, String salonId);

    Optional<Transaction> findByAppointmentId(String appointmentId);

    boolean existsByAppointmentId(String appointmentId);
}
