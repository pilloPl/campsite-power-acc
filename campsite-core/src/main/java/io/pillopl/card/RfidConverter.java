package io.pillopl.card;

import java.math.BigInteger;
import java.util.Locale;

/**
 * Conversions of card UIDs, the same rules as the database package RFID_UTILS: issuers deliver big-endian hex, the
 * pedestal readers send the decimal value of the bytes in reader order.
 */
public final class RfidConverter {

    private RfidConverter() {
        throw new AssertionError();
    }

    /**
     * "04A1B2C3" (big-endian hex from an issuer file) to the decimal UID a pedestal sends.
     */
    public static String hexBigEndianToDecimal(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        return new BigInteger(turnByteOrder(hex.trim()), 16).toString();
    }

    /**
     * The decimal UID a pedestal sends to big-endian hex (as printed in issuer files).
     */
    public static String decimalToHexBigEndian(String decimal) {
        if (decimal == null || decimal.isBlank()) {
            return null;
        }
        BigInteger value = new BigInteger(decimal.trim());
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Negative UID: " + decimal);
        }
        return turnByteOrder(value.toString(16));
    }

    /**
     * Reverses the order of the bytes (pairs of hex digits); an odd number of digits gets a leading zero.
     */
    public static String turnByteOrder(String hex) {
        String upper = hex.toUpperCase(Locale.ROOT);
        if (upper.length() % 2 == 1) {
            upper = "0" + upper;
        }
        StringBuilder result = new StringBuilder(upper.length());
        for (int i = upper.length() - 2; i >= 0; i -= 2) {
            result.append(upper, i, i + 2);
        }
        return result.toString();
    }

    /**
     * True for a decimal UID as the readers send it.
     */
    public static boolean isDecimalUid(String rfid) {
        if (rfid == null || rfid.isEmpty()) {
            return false;
        }
        for (int i = 0; i < rfid.length(); i++) {
            if (!Character.isDigit(rfid.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
