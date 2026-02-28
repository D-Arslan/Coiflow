package com.coiflow.dto.appointment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleRequest {

    @NotNull(message = "La nouvelle heure est obligatoire")
    private LocalDateTime startTime;

    private String barberId;
}
