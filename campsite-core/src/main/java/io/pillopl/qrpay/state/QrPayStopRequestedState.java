package io.pillopl.qrpay.state;

import io.pillopl.infrastructure.Pedestal;
import io.pillopl.qrpay.QrPayErrorCode;
import io.pillopl.qrpay.QrPayStateName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The app asked to stop; the stop is sent to the pedestal as its own step, so a stop the pedestal never got stays
 * visible.
 */
@Component
@Scope("prototype")
public class QrPayStopRequestedState extends QrPayState {

    @Override
    public QrPayState remoteSupplyStop() {
        if (process.getTransactionId() == null) {
            return handleError(QrPayErrorCode.SUPPLY_NOT_STARTED, "No transaction to stop for order "
                    + process.getOrderReference());
        }
        Pedestal pedestal = pedestalService.findBySocketId(process.getSocketId());
        if (pedestal == null || !remoteSupplyControl.remoteSupplyStop(pedestal, process.getTransactionId())) {
            return handleError(QrPayErrorCode.REMOTE_SUPPLY_STOP_FAILED, "Pedestal did not accept the remote stop of "
                    + "transaction " + process.getTransactionId());
        }
        return transitionTo(QrPayStateName.SOCKET_STOP_WAITING);
    }

    /**
     * The pedestal answers the remote stop before its transition is committed.
     */
    @Override
    public boolean awaitsOwnTransition() {
        return true;
    }
}
