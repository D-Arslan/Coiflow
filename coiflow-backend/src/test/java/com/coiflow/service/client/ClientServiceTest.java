package com.coiflow.service.client;

import com.coiflow.TestSecurityUtils;
import com.coiflow.dto.client.ClientResponse;
import com.coiflow.dto.client.CreateClientRequest;
import com.coiflow.dto.client.UpdateClientRequest;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.client.Client;
import com.coiflow.model.salon.Salon;
import com.coiflow.repository.client.ClientRepository;
import com.coiflow.repository.salon.SalonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.coiflow.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private SalonRepository salonRepository;

    @InjectMocks private ClientService clientService;

    @BeforeEach
    void setUp() {
        TestSecurityUtils.setTenantContext(SALON_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAll();
    }

    @Test
    void createClient_success() {
        Salon salon = aSalon();
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateClientRequest req = new CreateClientRequest();
        req.setFirstName("Mohamed");
        req.setLastName("Ben Ali");
        req.setPhone("0777000000");

        ClientResponse response = clientService.createClient(req);

        assertThat(response.getFirstName()).isEqualTo("Mohamed");
        assertThat(response.getLastName()).isEqualTo("Ben Ali");
    }

    @Test
    void getClients_noSearch() {
        Client c = aClient(SALON_ID);
        when(clientRepository.findBySalon_Id(SALON_ID)).thenReturn(List.of(c));

        List<ClientResponse> result = clientService.getClients(null);

        assertThat(result).hasSize(1);
        verify(clientRepository).findBySalon_Id(SALON_ID);
        verify(clientRepository, never()).findBySalon_IdAndLastNameContainingIgnoreCase(any(), any());
    }

    @Test
    void getClients_withSearch() {
        Client c = aClient(SALON_ID);
        when(clientRepository.findBySalon_IdAndLastNameContainingIgnoreCase(SALON_ID, "Client"))
                .thenReturn(List.of(c));

        List<ClientResponse> result = clientService.getClients("Client");

        assertThat(result).hasSize(1);
        verify(clientRepository).findBySalon_IdAndLastNameContainingIgnoreCase(SALON_ID, "Client");
    }

    @Test
    void updateClient_success() {
        Client existing = aClient(SALON_ID);
        when(clientRepository.findByIdAndSalon_Id(existing.getId(), SALON_ID))
                .thenReturn(Optional.of(existing));
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateClientRequest req = new UpdateClientRequest();
        req.setFirstName("Updated");
        req.setLastName("Name");
        req.setPhone("0888");

        ClientResponse response = clientService.updateClient(existing.getId(), req);

        assertThat(response.getFirstName()).isEqualTo("Updated");
    }

    @Test
    void updateClient_crossTenant_throws() {
        when(clientRepository.findByIdAndSalon_Id("client-other", SALON_ID))
                .thenReturn(Optional.empty());

        UpdateClientRequest req = new UpdateClientRequest();
        req.setFirstName("X");

        assertThatThrownBy(() -> clientService.updateClient("client-other", req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
