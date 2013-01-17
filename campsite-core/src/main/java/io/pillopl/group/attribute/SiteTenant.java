package io.pillopl.group.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Name of the operator of a SITE subtree (campsite operator), copied into consumption records.
 */
@Entity
@Table(name = "GRP_ATTR_SITE_TENANT")
@Getter
@Setter
public class SiteTenant extends GroupAttribute {

    @Column(name = "NAME", nullable = false, length = 256)
    private String name;

    public SiteTenant() {
    }

    public SiteTenant(String name) {
        this.name = name;
    }
}
