package io.pillopl.power;

import io.pillopl.value.SocketLinkId;

/**
 * Power budget of the section switchboard a socket hangs on, asked by the card authorization. The section power
 * module implements it (as primary bean): a socket without section has explicitly no limit, a section that cannot
 * be checked refuses (fail-closed).
 */
public interface SectionPowerBudget {

    /**
     * @return true when the section can take one more session at the socket
     */
    boolean isWithinSectionBudget(SocketLinkId socketLinkId);
}
