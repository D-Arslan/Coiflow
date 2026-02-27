package com.coiflow.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String salonId;
}
