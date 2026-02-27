package com.coiflow.repository.commission;

import com.coiflow.model.commission.Commission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommissionRepository extends JpaRepository<Commission, String> {

    List<Commission> findBySalonIdAndCreatedAtBetween(String salonId, LocalDateTime start, LocalDateTime end);

    List<Commission> findByBarberIdAndCreatedAtBetween(String barberId, LocalDateTime start, LocalDateTime end);

    Optional<Commission> findByTransactionId(String transactionId);
}
