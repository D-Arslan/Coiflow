package com.coiflow.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class DailyRevenueResponse {
    private String date;
    private BigDecimal revenue;
    private int transactionCount;
}
