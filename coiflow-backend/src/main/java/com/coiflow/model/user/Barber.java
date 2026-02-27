package com.coiflow.model.user;

import com.coiflow.model.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("BARBER")
@Getter
@Setter
@NoArgsConstructor
public class Barber extends Utilisateur {

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Override
    public Role getRole() {
        return Role.BARBER;
    }
}
