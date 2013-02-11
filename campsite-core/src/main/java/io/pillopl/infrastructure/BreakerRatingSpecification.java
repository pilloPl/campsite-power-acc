package io.pillopl.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Technical data of a breaker rating (reference data, seeded).
 */
@Entity
@Table(name = "BREAKER_RATING")
@Getter
@Setter
public class BreakerRatingSpecification {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "RATING", length = 8, nullable = false, unique = true)
    private BreakerRating rating;

    @Column(name = "NOMINAL_CURRENT_A", precision = 6, scale = 2, nullable = false)
    private BigDecimal nominalCurrentA;

    @Column(name = "NOMINAL_VOLTAGE_V", precision = 6, scale = 2, nullable = false)
    private BigDecimal nominalVoltageV;

    @Column(name = "MAXIMUM_POWER_KW", precision = 8, scale = 3, nullable = false)
    private BigDecimal maximumPowerKw;

    @Override
    public String toString() {
        return "BreakerRating{" + rating + ", " + maximumPowerKw + " kW}";
    }
}
