package io.pillopl.qrpay.state;

import io.pillopl.qrpay.QrPayStateName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The pedestal accepted the remote supply start; the supply begins when the guest plugs in.
 */
@Component
@Scope("prototype")
public class SocketStartWaitingState extends QrPayState {

    private static final Logger LOG = LoggerFactory.getLogger(SocketStartWaitingState.class);

    @Override
    public QrPayState supplyStarted(Long supplySessionId, Integer transactionId) {
        process.setSupplySessionId(supplySessionId);
        process.setTransactionId(transactionId);
        return transitionTo(QrPayStateName.SOCKET_START_RECEIVED);
    }

    @Override
    public QrPayState stopRequested() {
        LOG.info(String.format("Stop of QR payment %s requested while the socket waits for the guest",
                process.getProcessId()));
        return transitionTo(QrPayStateName.STOP_REQUESTED);
    }

    /**
     * A very short supply: its stop is handled before its start.
     */
    @Override
    public QrPayState supplyStopped(Long supplySessionId, Integer transactionId) {
        LOG.info(String.format("Socket of QR payment %s stopped before its start was handled",
                process.getProcessId()));
        process.setSupplySessionId(supplySessionId);
        process.setTransactionId(transactionId);
        return transitionTo(QrPayStateName.SOCKET_STOP_RECEIVED);
    }
}
