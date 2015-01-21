package io.pillopl.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrPayAuthTagTest {

    @Test
    void remoteSupplyStartAuthTagIsThePrefixAndTheProcessId() {
        assertThat(QrPayAuthTag.of(1005).value()).isEqualTo("QR1005");
    }

    @Test
    void authTagReportedByThePedestalGivesBackTheProcessId() {
        assertThat(QrPayAuthTag.parse(" qr1005 ")).isEqualTo(1005L);
    }

    @Test
    void cardUidsAreNoQrPaymentAuthTags() {
        assertThat(QrPayAuthTag.parse("04A2B3C4D5")).isNull();
        assertThat(QrPayAuthTag.parse("QR")).isNull();
        assertThat(QrPayAuthTag.parse("QR10A5")).isNull();
        assertThat(QrPayAuthTag.parse("QR0")).isNull();
        assertThat(QrPayAuthTag.parse("QR" + "9".repeat(19))).isNull();
        assertThat(QrPayAuthTag.parse(null)).isNull();
    }

    @Test
    void processIdMustBePositive() {
        assertThatThrownBy(() -> QrPayAuthTag.of(0)).isInstanceOf(InvalidValueException.class);
    }
}
