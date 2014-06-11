package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;

/**
 * Link v1: status of a socket (AVAILABLE, OCCUPIED, FAULTED, UNAVAILABLE) with the controller's error code.
 */
public record SocketStatusRequest(Integer socketNo, String status, String errorCode, String info)
        implements Serializable {
}
