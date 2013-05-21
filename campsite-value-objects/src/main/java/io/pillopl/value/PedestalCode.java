package io.pillopl.value;

import io.pillopl.commons.TextUtil;

/**
 * Code of a pedestal as printed on it, e.g. "HR-BAY-P001". Unique in the network. The asterisk is reserved as
 * separator of socket codes.
 */
public record PedestalCode(String value) {

    public static final int MAX_LENGTH = 128;

    public PedestalCode {
        value = TextUtil.trimToNull(value);
        if (value == null) {
            throw new InvalidValueException("PedestalCode", "is missing");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException("PedestalCode", "longer than " + MAX_LENGTH + " characters");
        }
        if (value.contains(SocketCode.SEPARATOR)) {
            throw new InvalidValueException("PedestalCode", "must not contain '" + SocketCode.SEPARATOR + "': "
                    + value);
        }
    }

    public static PedestalCode of(String value) {
        return new PedestalCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
