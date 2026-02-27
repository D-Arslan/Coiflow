package com.coiflow.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class DashboardStatsResponse {
    private BigDecimal revenueToday;
    private int appointmentsToday;
    private Map<String, Integer> appointmentsByStatus;
    private int activeBarbersCount;
}
