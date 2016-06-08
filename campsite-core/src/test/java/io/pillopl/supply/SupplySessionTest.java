package io.pillopl.supply;

import io.pillopl.card.CampingCard;
import io.pillopl.card.HouseCard;
import io.pillopl.card.HouseCardType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SupplySessionTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 3, 20, 0);

    @Test
    void cardOfTheSessionIsTheHouseCardBeforeTheCampingCard() {
        SupplySession session = new SupplySession();
        session.setCard(new CampingCard.Builder("7001", "HR-1").build());
        session.setHouseCard(new HouseCard(HouseCardType.STAFF, "9001", "S1"));

        assertThat(session.getCardRfid()).isEqualTo("9001");
        assertThat(session.isForHouseCard()).isTrue();
    }

    @Test
    void sessionOfAQrPaymentIsStartedWithItsAuthTag() {
        SupplySession session = new SupplySession();
        session.setQrPayProcessId(77L);

        assertThat(session.isForQrPay()).isTrue();
        assertThat(session.getCardRfid()).isEqualTo("QR77");
        assertThat(session.isStartedWith("QR77")).isTrue();
        assertThat(session.isStartedWith("7001")).isFalse();
    }

    @Test
    void sessionWithoutCardIsStartedWithNoCard() {
        assertThat(new SupplySession().isStartedWith(null)).isFalse();
    }

    @Test
    void staleTimeoutCountsFromTheCreateDateWhenThePedestalSentNoStartDate() {
        SupplySession session = new SupplySession();
        session.setActive(true);
        session.setCreateDate(NOW.minusHours(25));

        assertThat(session.isStale(NOW, 24)).isTrue();
        assertThat(session.isStale(NOW, 26)).isFalse();
    }

    @Test
    void closedSessionIsNeverStale() {
        SupplySession session = new SupplySession();
        session.setRemoteDate(NOW.minusDays(3));
        session.close(StopReason.REMOTE, NOW.minusDays(2));

        assertThat(session.isStale(NOW, 24)).isFalse();
        assertThat(session.getStopReason()).isEqualTo(StopReason.REMOTE);
        assertThat(session.getStopDate()).isEqualTo(NOW.minusDays(2));
    }
}
