package io.pillopl.group.attribute;

import io.pillopl.pricing.Tariff;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Default tariff of a campsite or section (SITE subtree): used for sockets and pedestals without an own tariff
 * assignment, before the default tariff of the market.
 */
@Entity
@Table(name = "GRP_ATTR_DEFAULT_TARIFF")
@Getter
@Setter
public class DefaultTariff extends GroupAttribute {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TARIFF_ID", nullable = false)
    private Tariff tariff;

    public DefaultTariff() {
    }

    public DefaultTariff(Tariff tariff) {
        this.tariff = tariff;
    }
}
