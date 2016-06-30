package io.pillopl.card;

import io.pillopl.infrastructure.Socket;

/**
 * Answers the validation of a QR payment auth tag (implemented by the QR payment): the pedestal reports a supply
 * started remotely for a payment process instead of a card.
 */
public interface QrPaySupplyAuthorizer {

    /**
     * @param supplyRunning the pedestal stopped the supply: only the process must exist at the socket; otherwise
     *                      it must wait for (or have just received) the start of the supply
     * @return true when the process belongs to the socket and allows the supply
     */
    boolean isSupplyAuthorized(Long qrPayProcessId, Socket socket, boolean supplyRunning);
}
