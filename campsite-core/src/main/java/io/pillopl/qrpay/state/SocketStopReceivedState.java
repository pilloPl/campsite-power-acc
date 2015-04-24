package io.pillopl.qrpay.state;

import io.pillopl.qrpay.QrPayStateName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The supply stopped; its consumption record is (being) written by the stop.
 */
@Component
@Scope("prototype")
public class SocketStopReceivedState extends QrPayState {

    @Override
    public QrPayState requestCalculation() {
        return transitionTo(QrPayStateName.CALCULATION_REQUESTED);
    }
}
