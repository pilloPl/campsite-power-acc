package io.pillopl.pedestallink.v1;

import io.pillopl.card.ValidationStatus;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.supply.StopReason;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedestalLinkServiceV1Test {

    @Test
    void linkV1KnowsFiveCardStatuses() {
        assertThat(PedestalLinkServiceV1.translateFrom(ValidationStatus.ACCEPTED)).isEqualTo("ACCEPTED");
        assertThat(PedestalLinkServiceV1.translateFrom(ValidationStatus.SOCKET_OCCUPIED)).isEqualTo("OCCUPIED");
        assertThat(PedestalLinkServiceV1.translateFrom(ValidationStatus.EXPIRED)).isEqualTo("EXPIRED");
        assertThat(PedestalLinkServiceV1.translateFrom(ValidationStatus.BLOCKED)).isEqualTo("BLOCKED");
        assertThat(PedestalLinkServiceV1.translateFrom(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE)).isEqualTo("INVALID");
        assertThat(PedestalLinkServiceV1.translateFrom(ValidationStatus.SOCKET_INVALID)).isEqualTo("INVALID");
        assertThat(PedestalLinkServiceV1.translateFrom(ValidationStatus.UNAVAILABLE)).isEqualTo("INVALID");
        assertThat(PedestalLinkServiceV1.translateFrom(null)).isEqualTo("INVALID");
    }

    @Test
    void occupiedSocketOfLinkV1IsASupplyingSocket() {
        assertThat(PedestalLinkServiceV1.translateStatus("OCCUPIED")).isEqualTo(SocketStatus.SUPPLYING);
        assertThat(PedestalLinkServiceV1.translateStatus("AVAILABLE")).isEqualTo(SocketStatus.AVAILABLE);
        assertThat(PedestalLinkServiceV1.translateStatus("FAULTED")).isEqualTo(SocketStatus.FAULTED);
        assertThat(PedestalLinkServiceV1.translateStatus("UNAVAILABLE")).isEqualTo(SocketStatus.UNAVAILABLE);
    }

    @Test
    void statusUnknownToLinkV1IsRefused() {
        assertThatThrownBy(() -> PedestalLinkServiceV1.translateStatus("BREAKER_TRIPPED"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PedestalLinkServiceV1.translateStatus(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void meterValuesOfLinkV1AreWattHours() {
        assertThat(PedestalLinkServiceV1.toKwh(1250500)).isEqualByComparingTo("1250.5");
        assertThat(PedestalLinkServiceV1.toKwh(null)).isNull();
    }

    @Test
    void stopReasonOfThePedestalIsReadLeniently() {
        assertThat(StopReason.fromPedestal("breaker-trip")).isEqualTo(StopReason.BREAKER_TRIP);
        assertThat(StopReason.fromPedestal(null)).isEqualTo(StopReason.LOCAL);
        assertThat(StopReason.fromPedestal("DE_AUTHORIZED")).isEqualTo(StopReason.OTHER);
    }
}
