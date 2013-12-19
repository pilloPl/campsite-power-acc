package io.pillopl.dto.card;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * One card on a local card list.
 */
public record LocalCardListEntryDTO(String rfid, LocalDate validTo, boolean houseCard) implements Serializable {
}
