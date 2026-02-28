package com.coiflow.service.catalog;

import com.coiflow.TestSecurityUtils;
import com.coiflow.dto.service.CreateServiceRequest;
import com.coiflow.dto.service.ServiceResponse;
import com.coiflow.dto.service.UpdateServiceRequest;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.catalog.ServiceItem;
import com.coiflow.model.salon.Salon;
import com.coiflow.repository.catalog.ServiceItemRepository;
import com.coiflow.repository.salon.SalonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.coiflow.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock private ServiceItemRepository serviceItemRepository;
    @Mock private SalonRepository salonRepository;

    @InjectMocks private ServiceCatalogService serviceCatalogService;

    @BeforeEach
    void setUp() {
        TestSecurityUtils.setTenantContext(SALON_ID);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAll();
    }

    @Test
    void createService_success() {
        Salon salon = aSalon();
        when(serviceItemRepository.existsBySalon_IdAndNameIgnoreCase(SALON_ID, "Coupe")).thenReturn(false);
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));
        when(serviceItemRepository.save(any(ServiceItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateServiceRequest req = new CreateServiceRequest();
        req.setName("Coupe");
        req.setDurationMinutes(30);
        req.setPrice(new BigDecimal("1500.00"));

        ServiceResponse response = serviceCatalogService.createService(req);

        assertThat(response.getName()).isEqualTo("Coupe");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void createService_duplicateName_throws() {
        when(serviceItemRepository.existsBySalon_IdAndNameIgnoreCase(SALON_ID, "Coupe")).thenReturn(true);

        CreateServiceRequest req = new CreateServiceRequest();
        req.setName("Coupe");

        assertThatThrownBy(() -> serviceCatalogService.createService(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nom existe deja");
    }

    @Test
    void getServices_activeOnly() {
        ServiceItem active = aServiceItem(new BigDecimal("500.00"), 15);
        when(serviceItemRepository.findBySalon_IdAndActive(SALON_ID, true))
                .thenReturn(List.of(active));

        List<ServiceResponse> result = serviceCatalogService.getServices();

        assertThat(result).hasSize(1);
        verify(serviceItemRepository).findBySalon_IdAndActive(SALON_ID, true);
    }

    @Test
    void deleteService_softDelete() {
        ServiceItem item = aServiceItem(new BigDecimal("500.00"), 15);
        when(serviceItemRepository.findByIdAndSalon_Id(item.getId(), SALON_ID))
                .thenReturn(Optional.of(item));
        when(serviceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        serviceCatalogService.deleteService(item.getId());

        assertThat(item.isActive()).isFalse();
    }

    @Test
    void updateService_notFound_throws() {
        when(serviceItemRepository.findByIdAndSalon_Id("unknown", SALON_ID))
                .thenReturn(Optional.empty());

        UpdateServiceRequest req = new UpdateServiceRequest();
        req.setName("X");

        assertThatThrownBy(() -> serviceCatalogService.updateService("unknown", req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
