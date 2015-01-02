package io.pillopl.guest;

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
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Camper, caravan or tent of a guest. Its plate goes to the supply sessions of the stays it is parked for.
 */
@Entity
@Table(name = "UNIT")
@Getter
@Setter
public class Unit {

    public enum UnitType {
        CAMPER,
        CARAVAN,
        TENT;

        /**
         * Tents carry no plate.
         */
        public boolean hasPlate() {
            return this != TENT;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_unit")
    @SequenceGenerator(name = "seq_unit", sequenceName = "UNIT_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "GUEST_ID", nullable = false)
    private Guest guest;

    @Enumerated(EnumType.STRING)
    @Column(name = "UNIT_TYPE", length = 16, nullable = false)
    private UnitType unitType;

    @Column(name = "PLATE", length = 32)
    private String plate;

    @Column(name = "LENGTH_M", precision = 4, scale = 1)
    private BigDecimal lengthM;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public Unit() {
    }

    public Unit(Guest guest, UnitType unitType, String plate, BigDecimal lengthM) {
        this.guest = guest;
        this.unitType = unitType;
        this.plate = unitType != null && unitType.hasPlate() ? normalizePlate(plate) : null;
        this.lengthM = lengthM;
    }

    /**
     * Upper case without blanks and dashes, as the barrier camera reads it.
     */
    public static String normalizePlate(String plate) {
        if (plate == null || plate.isBlank()) {
            return null;
        }
        return plate.replaceAll("[\\s-]", "").toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Unit other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Unit{id=" + id + ", " + unitType + ", plate='" + plate + "', length=" + lengthM + "}";
    }
}
