package io.pillopl.webservice.console.guest;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A camper, caravan or tent of the guest (tents carry no plate).
 */
public record AddUnitRequest(Long guestId, String unitType, String plate, BigDecimal lengthM) implements Serializable {
}
