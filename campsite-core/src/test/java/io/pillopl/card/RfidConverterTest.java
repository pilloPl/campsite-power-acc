package io.pillopl.card;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RfidConverterTest {

    @Test
    void bigEndianHexOfTheIssuerBecomesTheDecimalUidOfTheReader() {
        // bytes 04 A1 B2 C3 read in reverse order: C3B2A104
        assertThat(RfidConverter.hexBigEndianToDecimal("04A1B2C3")).isEqualTo("3283263748");
        assertThat(RfidConverter.hexBigEndianToDecimal("04a1b2c3")).isEqualTo("3283263748");
    }

    @Test
    void decimalUidGoesBackToTheSameHex() {
        assertThat(RfidConverter.decimalToHexBigEndian("3283263748")).isEqualTo("04A1B2C3");
        assertThat(RfidConverter.decimalToHexBigEndian(RfidConverter.hexBigEndianToDecimal("0A0B0C0D0E0F10")))
                .isEqualTo("0A0B0C0D0E0F10");
    }

    @Test
    void issuerUidWithOtherCharactersThanHexDigitsIsRefused() {
        assertThatThrownBy(() -> RfidConverter.hexBigEndianToDecimal("04:A1:B2:C3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not a hexadecimal UID: 04:A1:B2:C3");
        assertThatThrownBy(() -> RfidConverter.hexBigEndianToDecimal("-04A1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oddNumberOfDigitsGetsALeadingZero() {
        assertThat(RfidConverter.turnByteOrder("ABC")).isEqualTo("BC0A");
    }

    @Test
    void blankUidStaysEmptyAndNegativeIsRefused() {
        assertThat(RfidConverter.hexBigEndianToDecimal(" ")).isNull();
        assertThat(RfidConverter.decimalToHexBigEndian(null)).isNull();
        assertThatThrownBy(() -> RfidConverter.decimalToHexBigEndian("-5"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readersSendDecimalDigitsOnly() {
        assertThat(RfidConverter.isDecimalUid("3283263748")).isTrue();
        assertThat(RfidConverter.isDecimalUid("04A1B2C3")).isFalse();
        assertThat(RfidConverter.isDecimalUid("")).isFalse();
    }
}
