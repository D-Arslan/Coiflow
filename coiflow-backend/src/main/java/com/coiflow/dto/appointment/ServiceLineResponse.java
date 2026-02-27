package com.coiflow.dto.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class ServiceLineResponse {
    private String serviceId;
    private String serviceName;
    private BigDecimal priceApplied;
    private int durationMinutes;
}
