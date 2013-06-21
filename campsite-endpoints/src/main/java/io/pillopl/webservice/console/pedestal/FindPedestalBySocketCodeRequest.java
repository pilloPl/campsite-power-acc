package io.pillopl.webservice.console.pedestal;

import java.io.Serializable;

/**
 * Pedestal of a socket, by the code on the socket sticker.
 */
public record FindPedestalBySocketCodeRequest(String authUsername, String socketCode) implements Serializable {
}
