package io.pillopl.infrastructure;

import java.util.List;
import java.util.Locale;

/**
 * Socket codes: campsite code + pedestal + socket number, e.g. "HRBAY*P012*2". Given at commissioning, cleared when
 * the pedestal goes back to the supplier.
 */
public final class SocketCodeHelper {

    public static final String SEPARATOR = "*";

    private SocketCodeHelper() {
        throw new AssertionError();
    }

    public static String createSocketCode(String campsiteCode, String pedestalCode, Integer socketNo) {
        if (campsiteCode == null || campsiteCode.isBlank()) {
            throw new IllegalArgumentException("Campsite code is needed for a socket code of pedestal " + pedestalCode);
        }
        String pedestalPart = stripCampsitePrefix(normalize(pedestalCode), normalize(campsiteCode));
        return normalize(campsiteCode) + SEPARATOR + pedestalPart + SEPARATOR + socketNo;
    }

    /**
     * Sets the socket codes of all sockets of the pedestal from the campsite code of the location.
     */
    public static void setSocketCodes(Pedestal pedestal, Location location) {
        for (Socket socket : pedestal.getSockets()) {
            socket.setSocketCode(createSocketCode(location.getCampsiteCode(), pedestal.getCode(),
                    socket.getSocketNo() != null ? socket.getSocketNo() : socket.getPosition()));
        }
    }

    public static void setSocketCodesToNull(List<Socket> sockets) {
        if (sockets == null) {
            return;
        }
        for (Socket socket : sockets) {
            socket.setSocketCode(null);
        }
    }

    static String normalize(String s) {
        return s.trim().replace(" ", "").toUpperCase(Locale.ROOT);
    }

    /**
     * "HRBAY-P012" at campsite "HRBAY" gives "P012": the campsite is not repeated in the code.
     */
    static String stripCampsitePrefix(String pedestalCode, String campsiteCode) {
        if (pedestalCode.startsWith(campsiteCode + "-") || pedestalCode.startsWith(campsiteCode + SEPARATOR)) {
            return pedestalCode.substring(campsiteCode.length() + 1);
        }
        return pedestalCode;
    }
}
