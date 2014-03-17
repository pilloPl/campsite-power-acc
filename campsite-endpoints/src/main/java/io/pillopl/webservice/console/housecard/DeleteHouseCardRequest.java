package io.pillopl.webservice.console.housecard;

import java.io.Serializable;

/**
 * Deletes a house card of the owner's campsite.
 */
public record DeleteHouseCardRequest(String authUsername, String ownerUsername, Long houseCardId) implements Serializable {
}
