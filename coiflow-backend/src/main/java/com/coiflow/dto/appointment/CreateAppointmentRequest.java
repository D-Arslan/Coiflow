package com.coiflow.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateAppointmentRequest {

    @NotBlank(message = "Le coiffeur est obligatoire")
    private String barberId;

    private String clientId;

    @NotNull(message = "L'heure de debut est obligatoire")
    private LocalDateTime startTime;

    @NotEmpty(message = "Au moins une prestation est obligatoire")
    private List<String> serviceIds;

    private String notes;
}
