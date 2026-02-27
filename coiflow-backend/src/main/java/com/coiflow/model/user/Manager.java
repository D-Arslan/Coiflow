package com.coiflow.model.user;

import com.coiflow.model.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("MANAGER")
@Getter
@Setter
@NoArgsConstructor
public class Manager extends Utilisateur {

    @Override
    public Role getRole() {
        return Role.MANAGER;
    }
}
