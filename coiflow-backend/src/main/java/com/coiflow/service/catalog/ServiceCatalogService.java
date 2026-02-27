package com.coiflow.service.catalog;

import com.coiflow.dto.service.CreateServiceRequest;
import com.coiflow.dto.service.ServiceResponse;
import com.coiflow.dto.service.UpdateServiceRequest;
import com.coiflow.exception.ResourceNotFoundException;
import com.coiflow.model.catalog.ServiceItem;
import com.coiflow.model.salon.Salon;
import com.coiflow.repository.catalog.ServiceItemRepository;
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
public class ServiceCatalogService {

    private final ServiceItemRepository serviceItemRepository;
    private final SalonRepository salonRepository;

    private String requireSalonId() {
        String salonId = TenantContextHolder.getSalonId();
        if (salonId == null || salonId.isBlank()) {
            throw new IllegalStateException("Contexte de salon manquant");
        }
        return salonId;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'BARBER')")
    public List<ServiceResponse> getServices() {
        String salonId = requireSalonId();
        return serviceItemRepository.findBySalon_IdAndActive(salonId, true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public ServiceResponse createService(CreateServiceRequest request) {
        String salonId = requireSalonId();

        if (serviceItemRepository.existsBySalon_IdAndNameIgnoreCase(salonId, request.getName())) {
            throw new IllegalStateException("Une prestation avec ce nom existe deja");
        }

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable"));

        ServiceItem item = ServiceItem.builder()
                .id(UUID.randomUUID().toString())
                .salon(salon)
                .name(request.getName())
                .durationMinutes(request.getDurationMinutes())
                .price(request.getPrice())
                .active(true)
                .build();

        serviceItemRepository.save(item);
        return toResponse(item);
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public ServiceResponse updateService(String id, UpdateServiceRequest request) {
        String salonId = requireSalonId();
        ServiceItem item = serviceItemRepository.findByIdAndSalon_Id(id, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestation introuvable"));

        item.setName(request.getName());
        item.setDurationMinutes(request.getDurationMinutes());
        item.setPrice(request.getPrice());

        return toResponse(serviceItemRepository.save(item));
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteService(String id) {
        String salonId = requireSalonId();
        ServiceItem item = serviceItemRepository.findByIdAndSalon_Id(id, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestation introuvable"));
        item.setActive(false);
        serviceItemRepository.save(item);
    }

    private ServiceResponse toResponse(ServiceItem item) {
        return ServiceResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .durationMinutes(item.getDurationMinutes())
                .price(item.getPrice())
                .active(item.isActive())
                .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null)
                .build();
    }
}
