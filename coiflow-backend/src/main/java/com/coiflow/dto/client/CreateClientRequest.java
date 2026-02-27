package com.coiflow.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateClientRequest {

    @NotBlank(message = "Le prenom est requis")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    @Size(max = 50)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Email(message = "Format d'email invalide")
    @Size(max = 100)
    private String email;

    @Size(max = 500)
    private String notes;
}
