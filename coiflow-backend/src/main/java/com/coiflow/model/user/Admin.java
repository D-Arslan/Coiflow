package com.coiflow.model.user;

import com.coiflow.model.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("ADMIN")
@Getter
@Setter
@NoArgsConstructor
public class Admin extends Utilisateur {

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
