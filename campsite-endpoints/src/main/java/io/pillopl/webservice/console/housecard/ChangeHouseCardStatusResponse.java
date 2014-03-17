package io.pillopl.webservice.console.housecard;

import java.io.Serializable;

/**
 * The house card status after the change.
 */
public record ChangeHouseCardStatusResponse(Long houseCardId, boolean active) implements Serializable {
}
