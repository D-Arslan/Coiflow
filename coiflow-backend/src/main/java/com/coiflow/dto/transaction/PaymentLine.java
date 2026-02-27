package com.coiflow.dto.transaction;

import com.coiflow.model.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentLine {

    @NotNull(message = "Le mode de paiement est obligatoire")
    private PaymentMethod method;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit etre superieur a 0")
    private BigDecimal amount;
}
