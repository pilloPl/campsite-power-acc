package io.pillopl.supply;

import io.pillopl.infrastructure.Pedestal;

/**
 * Remote supply start and stop of the supply at a socket by the pedestal backend (the QR payment, the console's remote
 * control) and the service commands of the console. Implemented by the pedestal link client.
 */
public interface RemoteSupplyControl {

    /**
     * @return true when the pedestal accepted the start; the supply itself is reported by its supplyStarted
     */
    boolean remoteSupplyStart(Pedestal pedestal, int socketNo, String authTag);

    /**
     * @return true when the pedestal accepted the stop; the stop itself is reported by its supplyStopped
     */
    boolean remoteSupplyStop(Pedestal pedestal, int transactionId);

    /**
     * Asks the pedestal to report the status of a socket (null: of all its sockets) with its socketStatus messages.
     *
     * @return true when the pedestal accepted the request
     */
    boolean requestStatus(Pedestal pedestal, Integer socketNo);

    /**
     * Switches the tripped breaker of a socket on again (pedestal types with REMOTE_BREAKER_RESET).
     *
     * @return true when the pedestal accepted the reset; the socket reports its new status
     */
    boolean resetBreaker(Pedestal pedestal, int socketNo);
}
