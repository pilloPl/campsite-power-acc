package io.pillopl.qrpay.state;

import io.pillopl.qrpay.QrPayStateName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The cost is known; the app charges the guest with it.
 */
@Component
@Scope("prototype")
public class QrPayCalculationDoneState extends QrPayState {

    @Override
    public QrPayState close() {
        return transitionTo(QrPayStateName.CLOSED);
    }
}
