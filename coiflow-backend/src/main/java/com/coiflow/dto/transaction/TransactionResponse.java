package com.coiflow.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TransactionResponse {
    private String id;
    private String appointmentId;
    private String barberId;
    private String barberName;
    private BigDecimal totalAmount;
    private String status;
    private List<PaymentLineResponse> payments;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private String createdBy;
    private String createdAt;

    @Data
    @Builder
    @AllArgsConstructor
    public static class PaymentLineResponse {
        private String method;
        private BigDecimal amount;
    }
}
