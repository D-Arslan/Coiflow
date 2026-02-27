package com.coiflow.dto.staff;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateStaffRequest {

    @NotBlank(message = "Le prenom est requis")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
    private String password;

    @NotNull(message = "Le taux de commission est requis")
    @DecimalMin(value = "0.00", message = "Le taux ne peut pas etre negatif")
    @DecimalMax(value = "100.00", message = "Le taux ne peut pas depasser 100%")
    private BigDecimal commissionRate;
}
