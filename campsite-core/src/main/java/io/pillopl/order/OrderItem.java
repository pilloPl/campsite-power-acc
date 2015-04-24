package io.pillopl.order;

import io.pillopl.card.CampingCard;
import io.pillopl.group.Group;
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
import java.time.LocalDate;
import java.util.Objects;

/**
 * One product of an order with the price and the kWh package as ordered. A seasonal package names the campsite,
 * pitch and season; a card item the card handed out (or the number requested).
 */
@Entity
@Table(name = "ORDER_ITEM")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_order_item")
    @SequenceGenerator(name = "seq_order_item", sequenceName = "ORDER_ITEM_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private Order order;

    @Column(name = "POS", nullable = false)
    private int pos;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PRODUCT_DEFINITION_ID", nullable = false)
    private ProductDefinition productDefinition;

    @Column(name = "TITLE", length = 128, nullable = false)
    private String title;

    @Column(name = "QUANTITY", nullable = false)
    private int quantity = 1;

    @Column(name = "FIXED_PRICE", precision = 19, scale = 2)
    private BigDecimal fixedPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "FIXED_PRICE_PERIOD", length = 16)
    private ProductDefinition.FixedPricePeriod fixedPricePeriod;

    @Column(name = "PACKAGE_KWH", precision = 19, scale = 4)
    private BigDecimal packageKwh;

    @ManyToOne
    @JoinColumn(name = "SITE_GROUP_ID")
    private Group siteGroup;

    @Column(name = "PITCH_NO", length = 16)
    private String pitchNo;

    @Column(name = "START_DATE")
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "CARD_ID")
    private CampingCard card;

    @Column(name = "REQUESTED_CARD_NUMBER", length = 64)
    private String requestedCardNumber;

    public OrderItem() {
    }

    /**
     * The item as ordered: title, price and package copied from the catalogue.
     */
    OrderItem(Order order, int pos, ProductDefinition product, int quantity) {
        this.order = order;
        this.pos = pos;
        this.productDefinition = product;
        this.title = product.getTitle();
        this.quantity = quantity;
        this.fixedPrice = product.isHasFixedPrice() ? product.getFixedPrice() : null;
        this.fixedPricePeriod = product.isHasFixedPrice() ? product.getFixedPricePeriod() : null;
        this.packageKwh = product.getPackageKwh();
    }

    public boolean isSeasonalPackage() {
        return productDefinition.isSeasonalPackage();
    }

    /**
     * Fixed price of the item times the quantity; zero without a fixed price.
     */
    public BigDecimal getFixedAmount() {
        return fixedPrice != null ? fixedPrice.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "OrderItem{pos=" + pos + ", '" + title + "' x" + quantity + ", pitch=" + pitchNo + ", " + startDate
                + " - " + endDate + "}";
    }
}
