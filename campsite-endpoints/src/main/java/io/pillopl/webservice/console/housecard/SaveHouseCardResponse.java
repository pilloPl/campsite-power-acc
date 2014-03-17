package io.pillopl.webservice.console.housecard;

import io.pillopl.dto.card.HouseCardDTO;

import java.io.Serializable;

/**
 * The saved house card.
 */
public record SaveHouseCardResponse(HouseCardDTO houseCard) implements Serializable {
}
