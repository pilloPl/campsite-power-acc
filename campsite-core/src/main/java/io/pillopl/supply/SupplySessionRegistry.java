package io.pillopl.supply;

import io.pillopl.value.SocketLinkId;

/**
 * What the card authorization needs from the supply sessions: whether the socket already supplies for another card,
 * and announcing the session for an authorized card. The supply module implements it (as primary bean).
 */
public interface SupplySessionRegistry {

    /**
     * @return true when an active session on the socket belongs to another card
     */
    boolean hasConcurrentSession(SocketLinkId socketLinkId, String cardRfid);

    /**
     * The card was authorized at the socket; the session starts when the pedestal reports the supply.
     */
    void createSession(SocketLinkId socketLinkId, String cardRfid);
}
