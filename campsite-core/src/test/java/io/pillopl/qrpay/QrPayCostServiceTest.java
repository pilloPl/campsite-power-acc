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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QrPayCostServiceTest {

    static final LocalDateTime PLUG_IN = LocalDateTime.of(2026, 7, 10, 18, 0);

    final PricingService pricingService = mock(PricingService.class);
    final SupplySessionDao supplySessionDao = mock(SupplySessionDao.class);
    final ConsumptionRecordDao consumptionRecordDao = mock(ConsumptionRecordDao.class);
    final DateService dateService = new DateService();
    final QrPayCostService costService = new QrPayCostService(pricingService, supplySessionDao,
            consumptionRecordDao, dateService);

    final Socket socket = new Socket();
    final SupplySession session = new SupplySession();
    final QrPayProcess process = new QrPayProcess("ORD-1", 5L, "payment-app", "guest-7", PLUG_IN);

    @BeforeEach
    void setUp() {
        dateService.setClock(Clock.fixed(PLUG_IN.plusHours(2).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));
        socket.setId(5L);
        socket.setMeterKwh(new BigDecimal("110.5"));
        session.setId(77L);
        session.setSocket(socket);
        session.setStartMeterKwh(new BigDecimal("100.5"));
        session.setRemoteDate(PLUG_IN);
        session.setActive(true);
        process.setProcessId(1000L);
        when(supplySessionDao.findById(77L)).thenReturn(Optional.of(session));
    }

    @Test
    void beforeTheSupplyStartedThereIsNothingToPay() {
        QrPayCost cost = costService.calculatePreliminaryCost(process);

        assertThat(cost.cost()).isNull();
        assertThat(cost.finished()).isFalse();
        verifyNoInteractions(pricingService);
    }

    @Test
    void runningSupplyIsEstimatedWithTheSocketsTariffWithoutCard() {
        process.setSupplySessionId(77L);
        PriceCalculation calculation = priced("4.50");
        when(pricingService.calculateCost(any(PricingInput.class))).thenReturn(calculation);

        QrPayCost cost = costService.calculatePreliminaryCost(process);

        ArgumentCaptor<PricingInput> input = ArgumentCaptor.forClass(PricingInput.class);
        verify(pricingService).calculateCost(input.capture());
        assertThat(input.getValue().getCardId()).isNull();
        assertThat(input.getValue().getSocketId()).isEqualTo(5L);
        assertThat(input.getValue().getKwh()).isEqualByComparingTo("10");
        assertThat(input.getValue().getPlugOut()).isEqualTo(PLUG_IN.plusHours(2));
        assertThat(cost.cost()).isEqualByComparingTo("4.50");
        assertThat(cost.durationSeconds()).isEqualTo(7200L);
        assertThat(cost.finished()).isFalse();
    }

    @Test
    void estimateWithoutTariffHasNoAmountButTheReason() {
        process.setSupplySessionId(77L);
        PriceCalculation calculation = mock(PriceCalculation.class);
        when(calculation.isPriced()).thenReturn(false);
        when(calculation.getFailureReason()).thenReturn("No tariff");
        when(pricingService.calculateCost(any(PricingInput.class))).thenReturn(calculation);

        QrPayCost cost = costService.calculatePreliminaryCost(process);

        assertThat(cost.isPriced()).isFalse();
        assertThat(cost.failureReason()).isEqualTo("No tariff");
    }

    @Test
    void meterBelowTheStartCountsAsNoConsumptionYet() {
        socket.setMeterKwh(new BigDecimal("99"));

        assertThat(QrPayCostService.consumptionSoFar(session)).isEqualByComparingTo("0");
    }

    @Test
    void stoppedSupplyCostsWhatItsConsumptionRecordCosts() {
        process.setSupplySessionId(77L);
        session.setActive(false);
        when(consumptionRecordDao.findBySupplySessionId(77L)).thenReturn(record(PricingStatus.PRICED, "5.40"));

        QrPayCost cost = costService.calculatePreliminaryCost(process);

        assertThat(cost.cost()).isEqualByComparingTo("5.40");
        assertThat(cost.finished()).isTrue();
        verifyNoInteractions(pricingService);
    }

    @Test
    void recordNotWrittenYetHasNoFinalCost() {
        assertThat(costService.calculateCost(77L)).isNull();
        assertThat(costService.calculateCost(null)).isNull();
    }

    @Test
    void notBillableSupplyCostsNothing() {
        when(consumptionRecordDao.findBySupplySessionId(77L)).thenReturn(record(PricingStatus.NOT_BILLABLE, null));

        assertThat(costService.calculateCost(77L).cost()).isEqualByComparingTo("0.00");
    }

    @Test
    void failedPricingAndRecordsInReviewHaveNoFinalCost() {
        ConsumptionRecord failed = record(PricingStatus.PRICING_FAILED, null);
        failed.setPricingFailureReason("No element applies");
        when(consumptionRecordDao.findBySupplySessionId(77L)).thenReturn(failed);
        when(consumptionRecordDao.findBySupplySessionId(78L)).thenReturn(record(null, null));

        assertThat(costService.calculateCost(77L).failureReason()).isEqualTo("No element applies");
        assertThat(costService.calculateCost(78L).isPriced()).isFalse();
        assertThat(costService.calculateCost(78L).failureReason()).contains("waits for review");
    }

    private static PriceCalculation priced(String cost) {
        PriceCalculation calculation = mock(PriceCalculation.class);
        when(calculation.isPriced()).thenReturn(true);
        when(calculation.getCost()).thenReturn(new BigDecimal(cost));
        when(calculation.getCurrency()).thenReturn("EUR");
        return calculation;
    }

    private static ConsumptionRecord record(PricingStatus status, String cost) {
        ConsumptionRecord record = new ConsumptionRecord();
        record.setPricingStatus(status);
        record.setCost(cost != null ? new BigDecimal(cost) : null);
        record.setKwh(new BigDecimal("12"));
        record.setRecordNo("HRBAY-20260710-1");
        return record;
    }
}
