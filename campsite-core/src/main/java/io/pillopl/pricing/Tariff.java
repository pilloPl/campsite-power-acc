package io.pillopl.pricing;

import io.pillopl.group.Market;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The one price model of the network. A tariff of a market is a list of elements; every element holds price
 * components and an optional restriction saying when the element applies. The same tariff prices the start answer
 * to the pedestal, the QR payment, the consumption record and the seasonal ERP billing.
 * <p>
 * One tariff per market may be the default tariff, used when nothing more specific is assigned.
 */
@Entity
@Table(name = "TARIFF")
@Getter
@Setter
public class Tariff {

    public static final String DEFAULT_CURRENCY = "EUR";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tariff")
    @SequenceGenerator(name = "seq_tariff", sequenceName = "TARIFF_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", length = 128, nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", length = 512, nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET", length = 8, nullable = false)
    private Market market;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency = DEFAULT_CURRENCY;

    @Column(name = "DEFAULT_TARIFF", nullable = false)
    private boolean defaultTariff;

    @Column(name = "LUD", nullable = false)
    private LocalDateTime lastUpdateTime = LocalDateTime.now();

    @OneToMany(mappedBy = "tariff", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<TariffElement> tariffElements = new ArrayList<>();

    public Tariff() {
    }

    public Tariff(String name, String description, Market market) {
        this.name = name;
        this.description = description;
        this.market = market;
    }

    /**
     * Appends the element as the last one (elements are evaluated in their order).
     */
    public TariffElement addElement(TariffElement element) {
        element.setTariff(this);
        element.setPosition(tariffElements.size());
        tariffElements.add(element);
        return element;
    }

    /**
     * Links the elements, components and restrictions of a tariff built from a transfer object to their parents
     * and numbers the elements in their order.
     */
    public void fillTariffDetails() {
        int position = 0;
        for (TariffElement element : tariffElements) {
            element.setTariff(this);
            element.setPosition(position++);
            element.fillElementDetails();
        }
    }

    public Set<TariffDimensionType> getDimensionTypes() {
        Set<TariffDimensionType> result = EnumSet.noneOf(TariffDimensionType.class);
        for (TariffElement element : tariffElements) {
            for (PriceComponent component : element.getPriceComponents()) {
                result.add(component.getDimensionType());
            }
        }
        return result;
    }

    public boolean hasRestrictions() {
        for (TariffElement element : tariffElements) {
            if (element.getTariffRestriction() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Tariff{id=" + id + ", name='" + name + "', market=" + market + ", currency=" + currency
                + ", default=" + defaultTariff + ", elements=" + tariffElements.size() + "}";
    }
}
