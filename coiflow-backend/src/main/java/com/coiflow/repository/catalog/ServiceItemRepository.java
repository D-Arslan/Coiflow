package com.coiflow.repository.catalog;

import com.coiflow.model.catalog.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, String> {

    List<ServiceItem> findBySalon_IdAndActive(String salonId, boolean active);

    Optional<ServiceItem> findByIdAndSalon_Id(String id, String salonId);

    boolean existsBySalon_IdAndNameIgnoreCase(String salonId, String name);
}
