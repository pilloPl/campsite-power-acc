package io.pillopl.webservice.console.card;

import io.pillopl.dto.card.IssueCardDTO;

import java.io.Serializable;

/**
 * A card issued at the reception for a stay or a season.
 */
public record IssueCardRequest(IssueCardDTO card) implements Serializable {
}
