package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Link v1: answer to a supply stop: the status of the card that stopped (the controller caches it).
 */
public record SupplyStoppedResponse(String status, LocalDate expiryDate) implements Serializable {
}
