package io.pillopl.qrpay.state;

import io.pillopl.qrpay.QrPayCost;
import io.pillopl.qrpay.QrPayErrorCode;
import io.pillopl.qrpay.QrPayStateName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The price of the supply is taken from its consumption record: the QR payment and the record are the one settled
 * record (payment channel QR_APP), nothing is priced twice.
 */
@Component
@Scope("prototype")
public class QrPayCalculationRequestedState extends QrPayState {

    private static final Logger LOG = LoggerFactory.getLogger(QrPayCalculationRequestedState.class);

    @Override
    public QrPayState calculate() {
        QrPayCost cost = qrPayCostService.calculateCost(process.getSupplySessionId());
        if (cost == null) {
            LOG.debug(String.format("Consumption record of session %s not written yet", process.getSupplySessionId()));
            return this;
        }
        process.setKwh(cost.kwh());
        process.setDurationSeconds(cost.durationSeconds());
        if (!cost.isPriced()) {
            return handleError(QrPayErrorCode.CALCULATION_FAILED, cost.failureReason());
        }
        process.setCost(cost.cost());
        process.setCurrency(cost.currency());
        return transitionTo(QrPayStateName.CALCULATION_DONE);
    }
}
