package com.coiflow.dto.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateTransactionRequest {

    @NotBlank(message = "Le rendez-vous est obligatoire")
    private String appointmentId;

    @NotEmpty(message = "Au moins un paiement est obligatoire")
    private List<@Valid PaymentLine> payments;
}
