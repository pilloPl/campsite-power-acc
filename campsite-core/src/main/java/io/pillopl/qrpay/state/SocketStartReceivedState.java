package io.pillopl.qrpay.state;

import io.pillopl.qrpay.QrPayStateName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The socket supplies for the process; the start is not confirmed to the app yet.
 */
@Component
@Scope("prototype")
public class SocketStartReceivedState extends QrPayState {

    private static final Logger LOG = LoggerFactory.getLogger(SocketStartReceivedState.class);

    @Override
    public QrPayState confirmStart() {
        LOG.info(String.format("Supply of order %s started with session %s, transaction %s",
                process.getOrderReference(), process.getSupplySessionId(), process.getTransactionId()));
        return transitionTo(QrPayStateName.START_CONFIRMED);
    }

    @Override
    public QrPayState stopRequested() {
        return transitionTo(QrPayStateName.STOP_REQUESTED);
    }

    @Override
    public QrPayState supplyStopped(Long supplySessionId, Integer transactionId) {
        return transitionTo(QrPayStateName.SOCKET_STOP_RECEIVED);
    }
}
