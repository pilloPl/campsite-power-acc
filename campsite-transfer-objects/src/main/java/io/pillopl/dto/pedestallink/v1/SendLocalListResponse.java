package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;

/**
 * Link v1, answer of the pedestal: ACCEPTED, FAILED, VERSION_MISMATCH or NOT_SUPPORTED.
 */
public record SendLocalListResponse(String status) implements Serializable {

    public static final String ACCEPTED = "ACCEPTED";

    public boolean isAccepted() {
        return ACCEPTED.equals(status);
    }
}
