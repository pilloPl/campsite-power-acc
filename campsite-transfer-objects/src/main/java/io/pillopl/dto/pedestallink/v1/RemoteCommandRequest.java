package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;

/**
 * Link v1, pedestal backend to pedestal: start the supply of a socket with an auth tag (remoteSupplyStart) or stop the supply
 * of a transaction (remoteSupplyStop).
 */
public record RemoteCommandRequest(Integer socketNo, String authTag, Integer transactionId) implements Serializable {

    public static RemoteCommandRequest start(int socketNo, String authTag) {
        return new RemoteCommandRequest(socketNo, authTag, null);
    }

    public static RemoteCommandRequest stop(int transactionId) {
        return new RemoteCommandRequest(null, null, transactionId);
    }
}
