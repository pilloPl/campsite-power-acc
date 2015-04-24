package io.pillopl.order;

import io.pillopl.group.Market;
import io.pillopl.pricing.Tariff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.Objects;

/**
 * Product of the catalogue an order item is made of: a fixed price (once, monthly or for the season) and, for the
 * seasonal package, the kWh included and the tariff of the kWh above it. There is no unit price here: the kWh are
 * always priced by the tariff resolution.
 */
@Entity
@Table(name = "PRODUCT_DEFINITION")
@Getter
@Setter
public class ProductDefinition {

    public enum ProductType {
        SEASONAL_POWER_PACKAGE,
        TRANSIT_CARD,
        HOUSE_CARD_SET
    }

    public enum FixedPricePeriod {
        ONCE,
        MONTHLY,
        SEASONAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_product_definition")
    @SequenceGenerator(name = "seq_product_definition", sequenceName = "PRODUCT_DEFINITION_ID_SEQ",
            allocationSize = 1)
    private Long id;

    @Column(name = "CODE", length = 32, nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRODUCT_TYPE", length = 32, nullable = false)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET", length = 8, nullable = false)
    private Market market;

    @Column(name = "TITLE", length = 128, nullable = false)
    private String title;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "DURATION_MONTHS")
    private Integer durationMonths;

    @Column(name = "TERMINATION_NOTICE_DAYS")
    private Integer terminationNoticeDays;

    @Column(name = "HAS_BILLING", nullable = false)
    private boolean hasBilling = true;

    @Column(name = "HAS_FIXED_PRICE", nullable = false)
    private boolean hasFixedPrice;

    @Column(name = "FIXED_PRICE", precision = 19, scale = 2)
    private BigDecimal fixedPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "FIXED_PRICE_PERIOD", length = 16)
    private FixedPricePeriod fixedPricePeriod;

    @Column(name = "FIXED_PRICE_ERP_ARTICLE_NO", length = 32)
    private String fixedPriceErpArticleNo;

    @Column(name = "HAS_UNIT_PRICE", nullable = false)
    private boolean hasUnitPrice;

    @Column(name = "UNIT_PRICE_ERP_ARTICLE_NO", length = 32)
    private String unitPriceErpArticleNo;

    @Column(name = "PACKAGE_KWH", precision = 19, scale = 4)
    private BigDecimal packageKwh;

    /**
     * Tariff of the kWh above the package.
     */
    @ManyToOne
    @JoinColumn(name = "TARIFF_ID")
    private Tariff tariff;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency = "EUR";

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public ProductDefinition() {
    }

    public boolean isSeasonalPackage() {
        return productType == ProductType.SEASONAL_POWER_PACKAGE;
    }

    public boolean isCard() {
        return productType == ProductType.TRANSIT_CARD;
    }

    /**
     * Ordering the product makes a contract: the seasonal package.
     */
    public boolean createsContract() {
        return isSeasonalPackage() && hasBilling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductDefinition other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ProductDefinition{id=" + id + ", code='" + code + "', " + productType + "}";
    }
}
