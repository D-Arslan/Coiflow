package com.coiflow.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ClientResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String notes;
    private String createdAt;
}
