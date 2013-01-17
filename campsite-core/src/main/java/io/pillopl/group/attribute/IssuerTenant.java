package io.pillopl.group.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Name of the issuer of an ISSUER subtree (card issuer), copied into consumption records.
 */
@Entity
@Table(name = "GRP_ATTR_ISSUER_TENANT")
@Getter
@Setter
public class IssuerTenant extends GroupAttribute {

    @Column(name = "NAME", nullable = false, length = 256)
    private String name;

    public IssuerTenant() {
    }

    public IssuerTenant(String name) {
        this.name = name;
    }
}
