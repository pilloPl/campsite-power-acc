package io.pillopl.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Price of one dimension inside a tariff element.
 * <p>
 * Step size: ENERGY in Wh (default 1 Wh), TIME in seconds (e.g. 900 = quarter of an hour), PER_NIGHT in nights,
 * FLAT always 1. The quantity is rounded up to a whole number of steps before it is multiplied by the price.
 */
@Entity
@Table(name = "PRICE_COMPONENT")
@Getter
@Setter
public class PriceComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_price_component")
    @SequenceGenerator(name = "seq_price_component", sequenceName = "PRICE_COMPONENT_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TARIFF_ELEMENT_ID", nullable = false)
    private TariffElement tariffElement;

    @Enumerated(EnumType.STRING)
    @Column(name = "DIMENSION_TYPE", length = 16, nullable = false)
    private TariffDimensionType dimensionType;

    @Column(name = "PRICE", precision = 12, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "STEP_SIZE", nullable = false)
    private Integer stepSize = 1;

    public PriceComponent() {
    }

    public PriceComponent(TariffDimensionType dimensionType, BigDecimal price, Integer stepSize) {
        this.dimensionType = dimensionType;
        this.price = price;
        this.stepSize = stepSize;
    }

    public static PriceComponent energy(String pricePerKwh, int stepSizeInWh) {
        return new PriceComponent(TariffDimensionType.ENERGY, new BigDecimal(pricePerKwh), stepSizeInWh);
    }

    public static PriceComponent time(String pricePerHour, int stepSizeInSeconds) {
        return new PriceComponent(TariffDimensionType.TIME, new BigDecimal(pricePerHour), stepSizeInSeconds);
    }

    public static PriceComponent perNight(String pricePerNight) {
        return new PriceComponent(TariffDimensionType.PER_NIGHT, new BigDecimal(pricePerNight), 1);
    }

    public static PriceComponent flat(String pricePerSession) {
        return new PriceComponent(TariffDimensionType.FLAT, new BigDecimal(pricePerSession), 1);
    }

    public PriceUnit getPriceUnit() {
        return dimensionType != null ? dimensionType.getPriceUnit() : null;
    }

    @Override
    public String toString() {
        return "PriceComponent{id=" + id + ", " + dimensionType + ", price=" + price + ", stepSize=" + stepSize + "}";
    }
}
