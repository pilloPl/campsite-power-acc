package io.pillopl.qrpay.state;

import io.pillopl.qrpay.QrPayErrorCode;
import io.pillopl.qrpay.QrPayStateName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The supply runs and the app knows it. It ends with the app's stop or with the guest unplugging.
 */
@Component
@Scope("prototype")
public class QrPayStartConfirmedState extends QrPayState {

    private static final Logger LOG = LoggerFactory.getLogger(QrPayStartConfirmedState.class);

    @Override
    public QrPayState stopRequested() {
        return transitionTo(QrPayStateName.STOP_REQUESTED);
    }

    @Override
    public QrPayState supplyStopped(Long supplySessionId, Integer transactionId) {
        LOG.info(String.format("Supply of order %s stopped at the socket", process.getOrderReference()));
        return transitionTo(QrPayStateName.SOCKET_STOP_RECEIVED);
    }

    /**
     * A second start for the same process: the first supply keeps running, the start is only logged.
     */
    @Override
    public QrPayState supplyStarted(Long supplySessionId, Integer transactionId) {
        LOG.warn(String.format("QR payment %s: %s (session %s, now %s)", process.getProcessId(),
                QrPayErrorCode.SUPPLY_ALREADY_STARTED.getText(), process.getSupplySessionId(), supplySessionId));
        return this;
    }
}
