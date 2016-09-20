package io.pillopl.qrpay;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class QrPayCancelRuleTest {

    static final LocalDateTime STUCK_BEFORE = LocalDateTime.of(2026, 7, 10, 12, 0);

    @Test
    void closedPaymentIsNeverCancelled() {
        assertThat(QrPayProcessSupervisor.cancelRefusal(row(QrPayStateName.CLOSED, STUCK_BEFORE.minusHours(5)),
                true, STUCK_BEFORE)).isEqualTo("it is closed");
    }

    @Test
    void failedPaymentIsCancelledOnlyWhileItsSupplyRuns() {
        QrPayProcess error = row(QrPayStateName.ERROR, STUCK_BEFORE.plusMinutes(1));
        assertThat(QrPayProcessSupervisor.cancelRefusal(error, true, STUCK_BEFORE)).isNull();
        assertThat(QrPayProcessSupervisor.cancelRefusal(error, false, STUCK_BEFORE))
                .isEqualTo("it failed and no supply runs");
    }

    @Test
    void unfinishedPaymentIsCancelledOnlyOnceItIsStuck() {
        assertThat(QrPayProcessSupervisor.cancelRefusal(row(QrPayStateName.SOCKET_START_WAITING,
                STUCK_BEFORE.minusSeconds(1)), false, STUCK_BEFORE)).isNull();
        assertThat(QrPayProcessSupervisor.cancelRefusal(row(QrPayStateName.START_CONFIRMED, STUCK_BEFORE), true,
                STUCK_BEFORE)).startsWith("it is not stuck");
    }

    @Test
    void stateMachineHasNoWayOutOfClosedAndEveryOtherStateCanFail() {
        for (QrPayStateName state : QrPayStateName.values()) {
            if (state == QrPayStateName.CLOSED) {
                assertThat(state.getNextStates()).isEmpty();
            } else if (state != QrPayStateName.ERROR) {
                assertThat(state.getNextStates()).contains(QrPayStateName.ERROR);
            }
        }
        assertThat(QrPayStateName.ERROR.getNextStates()).containsExactly(QrPayStateName.STOP_REQUESTED,
                QrPayStateName.SOCKET_STOP_RECEIVED);
        assertThat(QrPayProcessSupervisor.transitions()).hasSize(25);
    }

    private static QrPayProcess row(QrPayStateName state, LocalDateTime startTime) {
        QrPayProcess process = new QrPayProcess("ORD", 1L, "app", "guest", startTime);
        process.setState(state.name());
        process.setStartTime(startTime);
        return process;
    }
}
