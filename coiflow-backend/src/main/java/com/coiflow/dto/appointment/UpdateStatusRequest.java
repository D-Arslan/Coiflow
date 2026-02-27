package com.coiflow.dto.appointment;

import com.coiflow.model.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private AppointmentStatus status;
}
