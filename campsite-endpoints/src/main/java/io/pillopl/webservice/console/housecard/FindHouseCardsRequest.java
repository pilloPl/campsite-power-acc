package io.pillopl.webservice.console.housecard;

import java.io.Serializable;

/**
 * House cards of the owner's campsite.
 */
public record FindHouseCardsRequest(String ownerUsername) implements Serializable {
}
