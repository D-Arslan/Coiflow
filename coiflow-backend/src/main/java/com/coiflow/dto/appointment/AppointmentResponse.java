package com.coiflow.dto.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class AppointmentResponse {
    private String id;
    private String barberId;
    private String barberName;
    private String clientId;
    private String clientName;
    private String startTime;
    private String endTime;
    private String status;
    private String notes;
    private List<ServiceLineResponse> services;
    private BigDecimal totalPrice;
    private String createdAt;
}
