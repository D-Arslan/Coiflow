package com.coiflow.dto.service;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateServiceRequest {

    @NotBlank(message = "Le nom de la prestation est requis")
    @Size(max = 100)
    private String name;

    @NotNull(message = "La duree est requise")
    @Min(value = 5, message = "La duree minimale est de 5 minutes")
    @Max(value = 480, message = "La duree maximale est de 480 minutes")
    private Integer durationMinutes;

    @NotNull(message = "Le prix est requis")
    @DecimalMin(value = "0.01", message = "Le prix doit etre positif")
    private BigDecimal price;
}
