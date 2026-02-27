package com.coiflow.repository.client;

import com.coiflow.model.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, String> {

    List<Client> findBySalon_Id(String salonId);

    List<Client> findBySalon_IdAndLastNameContainingIgnoreCase(String salonId, String lastName);

    Optional<Client> findByIdAndSalon_Id(String id, String salonId);
}
