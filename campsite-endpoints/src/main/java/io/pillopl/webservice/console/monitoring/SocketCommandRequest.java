package io.pillopl.webservice.console.monitoring;

import java.io.Serializable;

/**
 * A service command: requestStatus names the pedestal and optionally the socket number (null: all sockets);
 * resetFault and resendFaultMail name the socket by its id.
 */
public record SocketCommandRequest(String pedestalCode, Integer socketNo, Long socketId) implements Serializable {
}
