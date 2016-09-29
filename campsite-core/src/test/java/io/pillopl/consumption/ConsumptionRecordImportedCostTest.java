package io.pillopl.consumption;

import io.pillopl.pricing.PricingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumptionRecordImportedCostTest {

    @Test
    void importedRecordWithCostAndCurrencyIsPricedAndExported() {
        ConsumptionRecord record = imported(new BigDecimal("12.40"), "EUR");
        record.setPricingFailureReason("an older failure");

        record.applyPartnerPrice();

        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(record.getCost()).isEqualByComparingTo("12.40");
        assertThat(record.getPricingFailureReason()).isNull();
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
    }

    @Test
    void importedRecordWithoutCurrencyWaitsInReviewWithoutCost() {
        ConsumptionRecord record = imported(new BigDecimal("12.40"), " ");

        record.applyPartnerPrice();

        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(record.getCost()).isNull();
        assertThat(record.getPricingFailureReason()).isEqualTo(ConsumptionRecord.IMPORTED_WITHOUT_COST);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
    }

    @Test
    void importedRecordWithoutCostWaitsInReview() {
        ConsumptionRecord record = imported(null, "EUR");

        record.applyPartnerPrice();

        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
    }

    private static ConsumptionRecord imported(BigDecimal cost, String currency) {
        ConsumptionRecord record = new ConsumptionRecord();
        record.setSourceName("PARTNER");
        record.setExternalRecordNo("P-77");
        record.setCost(cost);
        record.setCurrency(currency);
        return record;
    }
}
