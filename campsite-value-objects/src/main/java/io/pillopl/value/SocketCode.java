package io.pillopl.value;

import io.pillopl.commons.TextUtil;

import java.util.Locale;

/**
 * Natural code of a socket: campsite code, pedestal part and socket number separated by an asterisk, e.g.
 * "HRBAY*P012*2". Given at commissioning, printed on the socket sticker.
 */
public record SocketCode(String campsiteCode, String pedestalPart, int socketNo) {

    public static final String SEPARATOR = "*";

    public static final int MAX_LENGTH = 64;

    public SocketCode {
        campsiteCode = TextUtil.trimToNull(campsiteCode);
        pedestalPart = TextUtil.trimToNull(pedestalPart);
        if (campsiteCode == null || pedestalPart == null) {
            throw new InvalidValueException("SocketCode", "campsite code and pedestal part are needed");
        }
        if (socketNo < 1) {
            throw new InvalidValueException("SocketCode", "socket number must be positive but is " + socketNo);
        }
        campsiteCode = campsiteCode.toUpperCase(Locale.ROOT);
        pedestalPart = pedestalPart.toUpperCase(Locale.ROOT);
    }

    /**
     * Reads "HRBAY*P012*2" (blanks around the parts and lower case are accepted).
     */
    public static SocketCode parse(String text) {
        String value = TextUtil.trimToNull(text);
        if (value == null) {
            throw new InvalidValueException("SocketCode", "is missing");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException("SocketCode", "longer than " + MAX_LENGTH + " characters");
        }
        String[] parts = value.split("\\" + SEPARATOR, -1);
        if (parts.length != 3) {
            throw new InvalidValueException("SocketCode", "expected campsite*pedestal*socket but got " + value);
        }
        try {
            return new SocketCode(parts[0], parts[1], Integer.parseInt(parts[2].trim()));
        } catch (NumberFormatException e) {
            throw new InvalidValueException("SocketCode", "socket number is not a number: " + parts[2]);
        }
    }

    public String value() {
        return campsiteCode + SEPARATOR + pedestalPart + SEPARATOR + socketNo;
    }

    @Override
    public String toString() {
        return value();
    }
}
