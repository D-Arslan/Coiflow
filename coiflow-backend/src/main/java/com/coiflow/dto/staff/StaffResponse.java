package com.coiflow.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class StaffResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private BigDecimal commissionRate;
    private boolean active;
    private String createdAt;
}
