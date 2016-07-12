package io.pillopl.webservice.console.housecard;

import io.pillopl.dto.card.HouseCardDTO;

import java.io.Serializable;

/**
 * Creates (without id) or changes a house card of the owner's campsite.
 */
public record SaveHouseCardRequest(String ownerUsername, HouseCardDTO houseCard) implements Serializable {
}
