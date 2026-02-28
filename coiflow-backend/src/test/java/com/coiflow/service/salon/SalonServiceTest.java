package com.coiflow.service.salon;

import com.coiflow.TestSecurityUtils;
import com.coiflow.dto.salon.CreateSalonRequest;
import com.coiflow.dto.salon.SalonResponse;
import com.coiflow.dto.salon.UpdateSalonRequest;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.mapper.SalonMapper;
import com.coiflow.model.salon.Salon;
import com.coiflow.model.user.Manager;
import com.coiflow.repository.salon.SalonRepository;
import com.coiflow.repository.user.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonServiceTest {

    @Mock private SalonRepository salonRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SalonMapper salonMapper;

    @InjectMocks private SalonService salonService;

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAll();
    }

    private CreateSalonRequest buildCreateRequest() {
        CreateSalonRequest req = new CreateSalonRequest();
        req.setName("Mon Salon");
        req.setAddress("123 Rue");
        req.setPhone("0555111111");
        req.setEmail("salon@test.com");
        req.setManagerFirstName("Karim");
        req.setManagerLastName("Gerant");
        req.setManagerEmail("karim@salon.com");
        req.setManagerPassword("password123");
        return req;
    }

    @Test
    void createSalon_success() {
        when(utilisateurRepository.existsByEmail("karim@salon.com")).thenReturn(false);
        when(salonRepository.save(any(Salon.class))).thenAnswer(inv -> inv.getArgument(0));
        when(utilisateurRepository.save(any(Manager.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(salonMapper.toResponse(any(Salon.class)))
                .thenReturn(SalonResponse.builder().id("id").name("Mon Salon").active(true).build());

        SalonResponse response = salonService.createSalon(buildCreateRequest());

        assertThat(response.getName()).isEqualTo("Mon Salon");
        assertThat(response.getManagerName()).isEqualTo("Karim Gerant");
        verify(salonRepository).save(any(Salon.class));

        ArgumentCaptor<Manager> captor = ArgumentCaptor.forClass(Manager.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("karim@salon.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void createSalon_duplicateEmail_throws() {
        when(utilisateurRepository.existsByEmail("karim@salon.com")).thenReturn(true);

        assertThatThrownBy(() -> salonService.createSalon(buildCreateRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email existe deja");
    }

    @Test
    void updateSalon_success() {
        Salon salon = Salon.builder().id("s-001").name("Old").active(true).build();
        when(salonRepository.findById("s-001")).thenReturn(Optional.of(salon));
        when(salonRepository.save(any(Salon.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salonMapper.toResponse(any(Salon.class)))
                .thenReturn(SalonResponse.builder().id("s-001").name("New Name").active(true).build());
        when(utilisateurRepository.findBySalonIdAndTypeAndActive("s-001", Manager.class, true))
                .thenReturn(java.util.List.of());

        UpdateSalonRequest req = new UpdateSalonRequest();
        req.setName("New Name");
        req.setAddress("456 Rue");
        req.setPhone("0666");
        req.setEmail("new@test.com");

        SalonResponse response = salonService.updateSalon("s-001", req);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(salon.getName()).isEqualTo("New Name");
    }

    @Test
    void toggleActive_success() {
        Salon salon = Salon.builder().id("s-001").name("Salon").active(true).build();
        when(salonRepository.findById("s-001")).thenReturn(Optional.of(salon));
        when(salonRepository.save(any(Salon.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salonMapper.toResponse(any(Salon.class)))
                .thenReturn(SalonResponse.builder().id("s-001").name("Salon").active(false).build());
        when(utilisateurRepository.findBySalonIdAndTypeAndActive("s-001", Manager.class, true))
                .thenReturn(java.util.List.of());

        salonService.toggleActive("s-001");

        assertThat(salon.isActive()).isFalse();
    }

    @Test
    void updateSalon_notFound_throws() {
        when(salonRepository.findById("unknown")).thenReturn(Optional.empty());

        UpdateSalonRequest req = new UpdateSalonRequest();
        req.setName("X");

        assertThatThrownBy(() -> salonService.updateSalon("unknown", req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
