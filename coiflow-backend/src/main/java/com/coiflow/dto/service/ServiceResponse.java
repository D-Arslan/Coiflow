package com.coiflow.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class ServiceResponse {
    private String id;
    private String name;
    private int durationMinutes;
    private BigDecimal price;
    private boolean active;
    private String createdAt;
}
