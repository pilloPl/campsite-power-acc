package io.pillopl.dto.pedestallink.v2;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Link v2 (and v3): status of a socket. Besides the v1 statuses the controller reports OCCUPIED (plugged in, no
 * supply), SUPPLYING, BREAKER_TRIPPED and FIRMWARE_UPDATE; next to the protocol error code it sends its own vendor
 * error code, its vendor id and the time of the status. Socket 0 is the pedestal itself (link v3 only).
 */
public record SocketStatusRequest(Integer socketNo, String status, String errorCode, String vendorId,
                                  String vendorErrorCode, String info, LocalDateTime timestamp)
        implements Serializable {
}
