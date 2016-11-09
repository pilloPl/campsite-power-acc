package io.pillopl.webservice.console.supply;

import java.io.Serializable;

/**
 * Remote supply start of a socket (map popup) for the UID of a camping card or house card valid there.
 */
public record StartSupplyRequest(Long socketId, String cardRfid) implements Serializable {
}
