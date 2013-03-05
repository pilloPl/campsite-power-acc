package io.pillopl.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Manufacturer and model of a pedestal: link generation, number of sockets and what the controller can do.
 * The capacity of the local card list comes from here (not from a constant).
 */
@Entity
@Table(name = "PEDESTAL_TYPE")
@Getter
@Setter
public class PedestalType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pedestal_type")
    @SequenceGenerator(name = "seq_pedestal_type", sequenceName = "PEDESTAL_TYPE_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", length = 128, nullable = false)
    private String name;

    @Column(name = "MANUFACTURER", length = 128, nullable = false)
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    @Column(name = "LINK_VERSION", length = 8, nullable = false)
    private PedestalLinkVersion linkVersion = PedestalLinkVersion.V1;

    @Column(name = "REMOTE_BREAKER_RESET", nullable = false)
    private boolean remoteBreakerReset;

    @Column(name = "LOCAL_CARD_LIST", nullable = false)
    private boolean localCardList;

    @Column(name = "LOCAL_CARD_LIST_CAPACITY")
    private Integer localCardListCapacity;

    @Column(name = "MAX_SOCKETS", nullable = false)
    private int maxSockets = 6;

    @Column(name = "DESCRIPTION", length = 50)
    private String description;

    @Column(name = "CODE")
    private Long code;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public PedestalType() {
    }

    public PedestalType(String manufacturer, String name, PedestalLinkVersion linkVersion) {
        this.manufacturer = manufacturer;
        this.name = name;
        this.linkVersion = linkVersion;
    }

    /**
     * @param defaultCapacity used when the type supports a local list but has no capacity configured
     * @return 0 when the controller keeps no local card list
     */
    public int getEffectiveLocalCardListCapacity(int defaultCapacity) {
        if (!localCardList) {
            return 0;
        }
        return localCardListCapacity != null ? localCardListCapacity : defaultCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PedestalType other)) return false;
        return Objects.equals(manufacturer, other.manufacturer) && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(manufacturer, name);
    }

    @Override
    public String toString() {
        return "PedestalType{id=" + id + ", " + manufacturer + " " + name + ", " + linkVersion + "}";
    }
}
