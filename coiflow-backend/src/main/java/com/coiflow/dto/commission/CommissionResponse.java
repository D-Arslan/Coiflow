package com.coiflow.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class CommissionResponse {
    private String id;
    private String barberId;
    private String barberName;
    private String transactionId;
    private BigDecimal rateApplied;
    private BigDecimal amount;
    private String createdAt;
}
