package io.pillopl.qrpay.state;

import io.pillopl.qrpay.QrPayStateName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Something went wrong. A supply that runs in spite of the error is still stopped and priced: the app may repeat
 * its stop, and the stop of the pedestal leads to the price.
 */
@Component
@Scope("prototype")
public class QrPayErrorState extends QrPayState {

    private static final Logger LOG = LoggerFactory.getLogger(QrPayErrorState.class);

    @Override
    public QrPayState stopRequested() {
        if (process.getTransactionId() == null) {
            // nothing supplies: there is nothing to stop
            return super.stopRequested();
        }
        LOG.info(String.format("Stop of QR payment %s repeated after error %s", process.getProcessId(),
                process.getErrorCode()));
        return transitionTo(QrPayStateName.STOP_REQUESTED);
    }

    @Override
    public QrPayState supplyStopped(Long supplySessionId, Integer transactionId) {
        if (process.getSupplySessionId() == null) {
            process.setSupplySessionId(supplySessionId);
            process.setTransactionId(transactionId);
        }
        return transitionTo(QrPayStateName.SOCKET_STOP_RECEIVED);
    }
}
