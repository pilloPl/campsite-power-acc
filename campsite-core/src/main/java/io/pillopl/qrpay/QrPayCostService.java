package io.pillopl.qrpay;

import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.pricing.PriceCalculation;
import io.pillopl.pricing.PricingInput;
import io.pillopl.pricing.PricingService;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.supply.SupplySession;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cost of a QR payment supply from the one tariff model: while the socket supplies an estimate of the tariff of the
 * socket (no card, so the socket / pedestal / campsite / market chain of the resolver applies) for the energy drawn
 * so far; after the stop the price of the supply's consumption record, the one record the guest pays.
 */
@Service
@Transactional(readOnly = true)
public class QrPayCostService {

    private static final Logger LOG = LoggerFactory.getLogger(QrPayCostService.class);

    private final PricingService pricingService;

    private final SupplySessionDao supplySessionDao;

    private final ConsumptionRecordDao consumptionRecordDao;

    private final DateService dateService;

    public QrPayCostService(PricingService pricingService, SupplySessionDao supplySessionDao,
                            ConsumptionRecordDao consumptionRecordDao, DateService dateService) {
        this.pricingService = pricingService;
        this.supplySessionDao = supplySessionDao;
        this.consumptionRecordDao = consumptionRecordDao;
        this.dateService = dateService;
    }

    /**
     * The cost the app shows for a process: nothing before the supply started, the estimate while it runs, the
     * price of the consumption record once it stopped.
     */
    public QrPayCost calculatePreliminaryCost(QrPayProcess process) {
        if (process.getSupplySessionId() == null) {
            return QrPayCost.nothingYet();
        }
        SupplySession session = supplySessionDao.findById(process.getSupplySessionId()).orElse(null);
        if (session == null) {
            return QrPayCost.nothingYet();
        }
        if (!session.isActive()) {
            QrPayCost cost = calculateCost(session.getId());
            return cost != null ? cost : QrPayCost.nothingYet();
        }
        final LocalDateTime start = session.getRemoteDate() != null ? session.getRemoteDate()
                : session.getCreateDate();
        final LocalDateTime now = dateService.getNow();
        final BigDecimal kwh = consumptionSoFar(session);
        final Long durationSeconds = start != null && !now.isBefore(start)
                ? Duration.between(start, now).getSeconds() : 0L;

        PriceCalculation calculation = pricingService.calculateCost(PricingInput.of(session.getSocket().getId(), null,
                start, now, kwh, null));
        if (!calculation.isPriced()) {
            LOG.info(String.format("No estimate for QR payment %s: %s", process.getProcessId(),
                    calculation.getFailureReason()));
            return QrPayCost.failed(kwh, durationSeconds, false, calculation.getFailureReason());
        }
        return new QrPayCost(kwh, durationSeconds, calculation.getCost(), calculation.getCurrency(), false, null);
    }

    /**
     * The final cost: the price of the consumption record of the session.
     *
     * @return null while the record is not written yet
     */
    public QrPayCost calculateCost(Long supplySessionId) {
        ConsumptionRecord record = supplySessionId != null
                ? consumptionRecordDao.findBySupplySessionId(supplySessionId) : null;
        if (record == null) {
            return null;
        }
        PricingStatus status = record.getPricingStatus();
        if (status == PricingStatus.PRICED) {
            return new QrPayCost(record.getKwh(), record.getDurationSeconds(), record.getCost(), record.getCurrency(),
                    true, null);
        }
        if (status == PricingStatus.NOT_BILLABLE) {
            // the pedestal is not billable or the socket free: nothing to pay
            return new QrPayCost(record.getKwh(), record.getDurationSeconds(),
                    BigDecimal.ZERO.setScale(PricingService.MONEY_SCALE), record.getCurrency(), true, null);
        }
        String reason = status == PricingStatus.PRICING_FAILED ? record.getPricingFailureReason()
                : "Consumption record " + record.getRecordNo() + " waits for review";
        return QrPayCost.failed(record.getKwh(), record.getDurationSeconds(), true, reason);
    }

    /**
     * Energy drawn so far: last reading of the socket minus the start meter; zero when one of them is unknown or
     * the difference is negative.
     */
    static BigDecimal consumptionSoFar(SupplySession session) {
        Socket socket = session.getSocket();
        BigDecimal start = session.getStartMeterKwh();
        BigDecimal last = socket != null ? socket.getMeterKwh() : null;
        if (start == null || last == null || last.compareTo(start) < 0) {
            return BigDecimal.ZERO;
        }
        return last.subtract(start);
    }
}
