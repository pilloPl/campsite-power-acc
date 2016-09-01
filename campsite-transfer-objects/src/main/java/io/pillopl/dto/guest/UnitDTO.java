package io.pillopl.dto.guest;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A camper, caravan or tent of a guest.
 */
public record UnitDTO(Long id, Long guestId, String unitType, String plate, BigDecimal lengthM, String description)
        implements Serializable {
}
