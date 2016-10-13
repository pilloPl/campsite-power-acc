package io.pillopl.supply;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SupplyRequestTest {

    static final LocalDateTime PLUG_IN = LocalDateTime.of(2026, 8, 2, 14, 0);
    static final LocalDateTime PLUG_OUT = LocalDateTime.of(2026, 8, 2, 19, 45);

    @Test
    void stopTakesTheStartValuesOfItsSessionAndKeepsItsOwnStopValues() {
        SupplyRequest stop = SupplyRequest.stop(1001, new BigDecimal("310.25"), PLUG_OUT, StopReason.UNPLUGGED);

        SupplyRequest withStart = stop.withStartOf(session());

        assertThat(withStart.getStartMeterKwh()).isEqualByComparingTo("300.5");
        assertThat(withStart.getStartTimestamp()).isEqualTo(PLUG_IN);
        assertThat(withStart.getTransactionId()).isEqualTo(1001);
        assertThat(withStart.getStopMeterKwh()).isEqualByComparingTo("310.25");
        assertThat(withStart.getStopTimestamp()).isEqualTo(PLUG_OUT);
        assertThat(withStart.getStopReason()).isEqualTo(StopReason.UNPLUGGED);
    }

    @Test
    void closeOfASessionTheSystemStopsUsesTheTransactionOfTheSession() {
        SupplyRequest close = SupplyRequest.close(session(), new BigDecimal("305"), PLUG_OUT, StopReason.TIMEOUT);

        assertThat(close.getTransactionId()).isEqualTo(1001);
        assertThat(close.getStartTimestamp()).isEqualTo(PLUG_IN);
        assertThat(close.getStopReason()).isEqualTo(StopReason.TIMEOUT);
    }

    @Test
    void startCarriesNoStopValues() {
        SupplyRequest start = SupplyRequest.start(new BigDecimal("300.5"), PLUG_IN);

        assertThat(start.getTransactionId()).isNull();
        assertThat(start.getStopMeterKwh()).isNull();
        assertThat(start.getStopReason()).isNull();
    }

    private static SupplySession session() {
        SupplySession session = new SupplySession();
        session.setTransactionId(1001);
        session.setStartMeterKwh(new BigDecimal("300.5"));
        session.setRemoteDate(PLUG_IN);
        return session;
    }
}
