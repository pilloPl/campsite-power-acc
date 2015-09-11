package io.pillopl.qrpay;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrPayProcessTest {

    static final LocalDateTime REQUESTED = LocalDateTime.of(2026, 7, 10, 18, 0);

    @Test
    void newProcessStartsRequested() {
        QrPayProcess process = process();

        assertThat(process.getStateName()).isEqualTo(QrPayStateName.START_REQUESTED);
        assertThat(process.getStartTime()).isEqualTo(REQUESTED);
        assertThat(process.isCurrent()).isTrue();
    }

    @Test
    void nextRowKeepsTheProcessAndPointsBackToItsPredecessor() {
        QrPayProcess process = process();
        process.setSupplySessionId(77L);
        process.setCost(new BigDecimal("4.50"));
        process.setStopTime(REQUESTED.plusMinutes(1));

        QrPayProcess next = process.copyForNextState(1002L, QrPayStateName.SOCKET_START_WAITING,
                REQUESTED.plusMinutes(1));

        assertThat(next.getId()).isEqualTo(1002L);
        assertThat(next.getProcessId()).isEqualTo(1000L);
        assertThat(next.getState()).isEqualTo("SOCKET_START_WAITING");
        assertThat(next.getPreviousProcessId()).isEqualTo(1000L);
        assertThat(next.getNextProcessId()).isNull();
        assertThat(next.getStopTime()).isNull();
        assertThat(next.getStartTime()).isEqualTo(REQUESTED.plusMinutes(1));
        assertThat(next.getRequestDate()).isEqualTo(REQUESTED);
        assertThat(next.getSupplySessionId()).isEqualTo(77L);
        assertThat(next.getCost()).isEqualByComparingTo("4.50");
    }

    @Test
    void detachedCopyIsTheRowAsItIs() {
        QrPayProcess process = process();
        process.setNextProcessId(1002L);
        process.setStopTime(REQUESTED.plusMinutes(1));

        QrPayProcess copy = process.detachedCopy();

        assertThat(copy).isNotSameAs(process);
        assertThat(copy.getId()).isEqualTo(1000L);
        assertThat(copy.getNextProcessId()).isEqualTo(1002L);
        assertThat(copy.getStopTime()).isEqualTo(REQUESTED.plusMinutes(1));
        assertThat(copy.isCurrent()).isFalse();
    }

    @Test
    void processBelongsToTheClientAndTheAppUserThatStartedIt() {
        QrPayProcess process = process();

        assertThat(process.isOwnedBy("PAYMENT-APP", "guest-7")).isTrue();
        assertThat(process.isOwnedBy("payment-app", "guest-8")).isFalse();
        assertThat(process.isOwnedBy("other-app", "guest-7")).isFalse();
        assertThat(process.isOwnedBy("payment-app", null)).isFalse();
    }

    @Test
    void stateNamesKnowWhereStopsAreIgnoredAndSuppliesMayStart() {
        assertThat(QrPayStateName.SOCKET_STOP_WAITING.isIgnoreStopRequest()).isTrue();
        assertThat(QrPayStateName.CLOSED.isIgnoreStopRequest()).isTrue();
        assertThat(QrPayStateName.START_CONFIRMED.isIgnoreStopRequest()).isFalse();
        assertThat(QrPayStateName.ERROR.isIgnoreStopRequest()).isFalse();
        assertThat(QrPayStateName.SOCKET_START_WAITING.isSupplyMayStart()).isTrue();
        assertThat(QrPayStateName.STOP_REQUESTED.isSupplyMayStart()).isFalse();
        assertThat(QrPayStateName.ERROR.isSupplyMayStart()).isFalse();
        assertThat(QrPayStateName.ERROR.isFinished()).isTrue();
        assertThat(QrPayStateName.CALCULATION_DONE.isFinished()).isFalse();
    }

    @Test
    void unknownStateNameIsRefused() {
        assertThat(QrPayStateName.fromString(null)).isNull();
        assertThatThrownBy(() -> QrPayStateName.fromString("PAID")).isInstanceOf(IllegalArgumentException.class);
    }

    private static QrPayProcess process() {
        QrPayProcess process = new QrPayProcess("ORD-1", 5L, "payment-app", "guest-7", REQUESTED);
        process.setId(1000L);
        process.setProcessId(1000L);
        return process;
    }
}
