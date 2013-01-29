package io.pillopl.group.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Whether pedestals of a SITE subtree may adopt a welcome card from the welcome card pool on its first use.
 */
@Entity
@Table(name = "GRP_ATTR_WELCOME_CARD_ADOPTION")
@Getter
@Setter
public class WelcomeCardAdoption extends GroupAttribute {

    @Column(name = "ALLOWED", nullable = false)
    private boolean allowed;

    public WelcomeCardAdoption() {
    }

    public WelcomeCardAdoption(boolean allowed) {
        this.allowed = allowed;
    }
}
