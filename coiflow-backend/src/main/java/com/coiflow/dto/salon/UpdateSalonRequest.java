package com.coiflow.dto.salon;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSalonRequest {

    @NotBlank(message = "Le nom du salon est requis")
    @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres")
    private String name;

    @Size(max = 255)
    private String address;

    @Size(max = 20)
    private String phone;

    @Email(message = "Format d'email invalide")
    @Size(max = 100)
    private String email;
}
