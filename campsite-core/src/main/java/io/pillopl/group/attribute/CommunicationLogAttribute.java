package io.pillopl.group.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Whether the pedestal link traffic of a SITE subtree is written to the communication log.
 */
@Entity
@Table(name = "GRP_ATTR_COMMLOG")
@Getter
@Setter
public class CommunicationLogAttribute extends GroupAttribute {

    @Column(name = "NAME", nullable = false, length = 256)
    private String name;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active;

    public CommunicationLogAttribute() {
    }

    public CommunicationLogAttribute(String name, boolean active) {
        this.name = name;
        this.active = active;
    }
}
