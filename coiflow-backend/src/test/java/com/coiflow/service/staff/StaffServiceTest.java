package com.coiflow.service.staff;

import com.coiflow.TestSecurityUtils;
import com.coiflow.dto.staff.CreateStaffRequest;
import com.coiflow.dto.staff.StaffResponse;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.salon.Salon;
import com.coiflow.model.user.Barber;
import com.coiflow.repository.salon.SalonRepository;
import com.coiflow.repository.user.UtilisateurRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static com.coiflow.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private SalonRepository salonRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private StaffService staffService;

    @BeforeEach
    void setUp() {
        TestSecurityUtils.setTenantContext(SALON_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAll();
    }

    @Test
    void createStaff_success() {
        Salon salon = aSalon();
        when(utilisateurRepository.existsByEmail("ali@barber.com")).thenReturn(false);
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(utilisateurRepository.save(any(Barber.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateStaffRequest req = new CreateStaffRequest();
        req.setFirstName("Ali");
        req.setLastName("Coiffeur");
        req.setEmail("ali@barber.com");
        req.setPassword("password123");
        req.setCommissionRate(new BigDecimal("25.00"));

        StaffResponse response = staffService.createStaff(req);

        assertThat(response.getFirstName()).isEqualTo("Ali");
        assertThat(response.getCommissionRate()).isEqualByComparingTo(new BigDecimal("25.00"));

        ArgumentCaptor<Barber> captor = ArgumentCaptor.forClass(Barber.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getSalon().getId()).isEqualTo(SALON_ID);
    }

    @Test
    void createStaff_duplicateEmail_throws() {
        when(utilisateurRepository.existsByEmail("existing@test.com")).thenReturn(true);

        CreateStaffRequest req = new CreateStaffRequest();
        req.setEmail("existing@test.com");

        assertThatThrownBy(() -> staffService.createStaff(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email existe deja");
    }

    @Test
    void deleteStaff_softDelete() {
        Barber barber = aBarber(SALON_ID);
        when(utilisateurRepository.findByIdAndType(barber.getId(), Barber.class))
                .thenReturn(Optional.of(barber));
        when(utilisateurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        staffService.deleteStaff(barber.getId());

        assertThat(barber.isActive()).isFalse();
        verify(utilisateurRepository).save(barber);
    }

    @Test
    void getStaff_missingContext_throws() {
        TestSecurityUtils.clearAll(); // No tenant context

        assertThatThrownBy(() -> staffService.getStaff())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("salon manquant");
    }

    @Test
    void updateStaff_crossTenant_throws() {
        // Barber belongs to OTHER_SALON, but tenant context is SALON_ID
        Barber otherBarber = aBarber(OTHER_SALON_ID);
        when(utilisateurRepository.findByIdAndType(otherBarber.getId(), Barber.class))
                .thenReturn(Optional.of(otherBarber));

        assertThatThrownBy(() -> staffService.deleteStaff(otherBarber.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
