package io.pillopl.guest;

import io.pillopl.order.OrderItem;
import io.pillopl.order.ProductDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A product of a seasonal contract with the kWh package it brings, from the order item it was ordered with.
 */
@Entity
@Table(name = "PACKAGE_ITEM")
@Getter
@Setter
public class PackageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_package_item")
    @SequenceGenerator(name = "seq_package_item", sequenceName = "PACKAGE_ITEM_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "CONTRACT_ID", nullable = false)
    private SeasonalContract contract;

    @ManyToOne
    @JoinColumn(name = "ORDER_ITEM_ID")
    private OrderItem orderItem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PRODUCT_DEFINITION_ID", nullable = false)
    private ProductDefinition productDefinition;

    @Column(name = "PACKAGE_KWH", precision = 19, scale = 4, nullable = false)
    private BigDecimal packageKwh = BigDecimal.ZERO;

    public PackageItem() {
    }

    PackageItem(SeasonalContract contract, ProductDefinition product, OrderItem orderItem, BigDecimal packageKwh) {
        this.contract = contract;
        this.productDefinition = product;
        this.orderItem = orderItem;
        this.packageKwh = packageKwh != null ? packageKwh : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackageItem other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "PackageItem{id=" + id + ", product=" + (productDefinition != null ? productDefinition.getCode() : null)
                + ", kWh=" + packageKwh + "}";
    }
}
