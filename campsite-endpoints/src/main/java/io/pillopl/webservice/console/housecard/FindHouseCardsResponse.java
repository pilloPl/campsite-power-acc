package io.pillopl.webservice.console.housecard;

import io.pillopl.dto.card.HouseCardDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The house cards (deactivated ones too).
 */
public record FindHouseCardsResponse(List<HouseCardDTO> houseCards) implements Serializable {
}
