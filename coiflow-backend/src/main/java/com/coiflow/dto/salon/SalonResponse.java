package com.coiflow.dto.salon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SalonResponse {
    private String id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private boolean active;
    private String managerId;
    private String managerName;
    private String createdAt;
}
