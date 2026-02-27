package com.coiflow.repository.salon;

import com.coiflow.model.salon.Salon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonRepository extends JpaRepository<Salon, String> {

    List<Salon> findByActive(boolean active);
}
