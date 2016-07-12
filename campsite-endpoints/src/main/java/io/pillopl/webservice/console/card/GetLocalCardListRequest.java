package io.pillopl.webservice.console.card;

import java.io.Serializable;

/**
 * The local card list a pedestal would get now.
 */
public record GetLocalCardListRequest(String pedestalCode) implements Serializable {
}
