package com.coiflow.repository.appointment;

import com.coiflow.model.appointment.Appointment;
import com.coiflow.model.transaction.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    List<Appointment> findBySalonIdAndStartTimeBetween(String salonId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findBySalonIdAndBarberIdAndStartTimeBetween(String salonId, String barberId, LocalDateTime start, LocalDateTime end);

    Optional<Appointment> findByIdAndSalonId(String id, String salonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.barber.id = :barberId
          AND a.status <> com.coiflow.model.enums.AppointmentStatus.CANCELLED
          AND a.status <> com.coiflow.model.enums.AppointmentStatus.NO_SHOW
          AND a.startTime < :newEnd
          AND a.endTime > :newStart
        """)
    List<Appointment> findOverlappingForUpdate(
            @Param("barberId") String barberId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd);

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.salon.id = :salonId
          AND a.status = com.coiflow.model.enums.AppointmentStatus.COMPLETED
          AND NOT EXISTS (SELECT t FROM Transaction t WHERE t.appointment.id = a.id)
          AND a.startTime >= :start
          AND a.startTime < :end
        """)
    List<Appointment> findToCash(
            @Param("salonId") String salonId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
