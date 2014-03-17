package io.pillopl.webservice.console.housecard;

import java.io.Serializable;

/**
 * Activates or deactivates a house card.
 */
public record ChangeHouseCardStatusRequest(String authUsername, String ownerUsername, Long houseCardId, Boolean active) implements Serializable {
}
