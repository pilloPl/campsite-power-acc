package io.pillopl.pricing;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Element of a tariff: price components valid together, optionally only inside the window of a restriction.
 */
@Entity
@Table(name = "TARIFF_ELEMENT")
@Getter
@Setter
public class TariffElement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tariff_element")
    @SequenceGenerator(name = "seq_tariff_element", sequenceName = "TARIFF_ELEMENT_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TARIFF_ID", nullable = false)
    private Tariff tariff;

    @Column(name = "POS", nullable = false)
    private int position;

    @OneToMany(mappedBy = "tariffElement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PriceComponent> priceComponents = new ArrayList<>();

    @OneToOne(mappedBy = "tariffElement", cascade = CascadeType.ALL, orphanRemoval = true)
    private TariffRestriction tariffRestriction;

    public TariffElement() {
    }

    public TariffElement addComponent(PriceComponent component) {
        component.setTariffElement(this);
        priceComponents.add(component);
        return this;
    }

    public TariffElement restrictTo(TariffRestriction restriction) {
        if (restriction != null) {
            restriction.setTariffElement(this);
        }
        this.tariffRestriction = restriction;
        return this;
    }

    void fillElementDetails() {
        for (PriceComponent component : priceComponents) {
            component.setTariffElement(this);
        }
        if (tariffRestriction != null) {
            tariffRestriction.setTariffElement(this);
        }
    }

    /**
     * @return the component of the dimension, or null when the element has none
     */
    public PriceComponent findComponent(TariffDimensionType dimensionType) {
        for (PriceComponent component : priceComponents) {
            if (component.getDimensionType() == dimensionType) {
                return component;
            }
        }
        return null;
    }

    /**
     * An element without restriction applies always.
     *
     * @param sessionKwh energy of the whole session, checked against the kWh range of the restriction
     */
    public boolean appliesAt(LocalDateTime at, BigDecimal sessionKwh) {
        return tariffRestriction == null || tariffRestriction.matches(at, sessionKwh);
    }

    @Override
    public String toString() {
        return "TariffElement{id=" + id + ", position=" + position + ", components=" + priceComponents
                + ", restriction=" + tariffRestriction + "}";
    }
}
