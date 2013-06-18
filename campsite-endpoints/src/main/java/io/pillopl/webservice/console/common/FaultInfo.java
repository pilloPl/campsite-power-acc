package io.pillopl.webservice.console.common;

import java.io.Serializable;

/**
 * Body of a failed web service call: error code and a message for the console user.
 */
public record FaultInfo(WsErrorCode errorCode, String message) implements Serializable {
}
