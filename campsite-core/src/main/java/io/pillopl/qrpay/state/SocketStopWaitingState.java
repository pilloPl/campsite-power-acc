package io.pillopl.qrpay.state;

import io.pillopl.qrpay.QrPayStateName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The pedestal accepted the remote supply stop; its supplyStopped follows.
 */
@Component
@Scope("prototype")
public class SocketStopWaitingState extends QrPayState {

    @Override
    public QrPayState supplyStopped(Long supplySessionId, Integer transactionId) {
        return transitionTo(QrPayStateName.SOCKET_STOP_RECEIVED);
    }
}
