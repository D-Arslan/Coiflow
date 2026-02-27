package com.coiflow.dto.salon;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSalonRequest {

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

    @NotBlank(message = "Le prenom du gerant est requis")
    @Size(max = 50)
    private String managerFirstName;

    @NotBlank(message = "Le nom du gerant est requis")
    @Size(max = 50)
    private String managerLastName;

    @NotBlank(message = "L'email du gerant est requis")
    @Email(message = "Format d'email invalide")
    private String managerEmail;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
    private String managerPassword;
}
