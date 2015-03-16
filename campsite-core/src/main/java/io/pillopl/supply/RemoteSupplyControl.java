package io.pillopl.supply;

import io.pillopl.infrastructure.Pedestal;

/**
 * Remote start and stop of the supply at a socket by the pedestal backend (the QR payment, later the console's remote
 * control). Implemented by the pedestal link client.
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
}
