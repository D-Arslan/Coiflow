package com.coiflow.service.client;

import com.coiflow.dto.client.ClientResponse;
import com.coiflow.dto.client.CreateClientRequest;
import com.coiflow.dto.client.UpdateClientRequest;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.client.Client;
import com.coiflow.model.salon.Salon;
import com.coiflow.repository.client.ClientRepository;
import com.coiflow.repository.salon.SalonRepository;
import com.coiflow.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final SalonRepository salonRepository;

    private String requireSalonId() {
        String salonId = TenantContextHolder.getSalonId();
        if (salonId == null || salonId.isBlank()) {
            throw new IllegalStateException("Contexte de salon manquant");
        }
        return salonId;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'BARBER')")
    public List<ClientResponse> getClients(String search) {
        String salonId = requireSalonId();
        List<Client> clients = (search != null && !search.isBlank())
                ? clientRepository.findBySalon_IdAndLastNameContainingIgnoreCase(salonId, search.trim())
                : clientRepository.findBySalon_Id(salonId);
        return clients.stream().map(this::toResponse).toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'BARBER')")
    public ClientResponse createClient(CreateClientRequest request) {
        String salonId = requireSalonId();

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable"));

        Client client = Client.builder()
                .id(UUID.randomUUID().toString())
                .salon(salon)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .notes(request.getNotes())
                .build();

        clientRepository.save(client);
        return toResponse(client);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'BARBER')")
    public ClientResponse updateClient(String id, UpdateClientRequest request) {
        String salonId = requireSalonId();
        Client client = clientRepository.findByIdAndSalon_Id(id, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));

        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setPhone(request.getPhone());
        client.setEmail(request.getEmail());
        client.setNotes(request.getNotes());

        return toResponse(clientRepository.save(client));
    }

    private ClientResponse toResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .phone(client.getPhone())
                .email(client.getEmail())
                .notes(client.getNotes())
                .createdAt(client.getCreatedAt() != null ? client.getCreatedAt().toString() : null)
                .build();
    }
}
