package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;

/**
 * Link v1, answer of the pedestal to a remote start or stop: ACCEPTED or REJECTED.
 */
public record RemoteCommandResponse(String status) implements Serializable {

    public static final String ACCEPTED = "ACCEPTED";

    public boolean isAccepted() {
        return ACCEPTED.equals(status);
    }
}
