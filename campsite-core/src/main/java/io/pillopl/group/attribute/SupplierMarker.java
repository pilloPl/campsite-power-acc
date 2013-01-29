package io.pillopl.group.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Marks a SITE subtree as the pedestal stock of a manufacturer ("SUPPLIER &lt;manufacturer&gt;"): pedestals there are delivered but not commissioned.
 */
@Entity
@Table(name = "GRP_ATTR_SUPPLIER")
@Getter
@Setter
public class SupplierMarker extends GroupAttribute {

    @Column(name = "NAME", nullable = false, length = 256)
    private String name;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active;

    public SupplierMarker() {
    }

    public SupplierMarker(String name, boolean active) {
        this.name = name;
        this.active = active;
    }
}
