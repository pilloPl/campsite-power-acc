package io.pillopl.webservice.console.housecard;

import java.io.Serializable;

/**
 * The deleted house card.
 */
public record DeleteHouseCardResponse(Long houseCardId, boolean deleted) implements Serializable {
}
