package io.pillopl.consumption;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumptionRecordValuesTest {

    @Test
    void houseCardSupplyIsPaidAsHouseCardEvenWithACard() {
        assertThat(PaymentChannel.of(true, true)).isEqualTo(PaymentChannel.HOUSE_CARD);
    }

    @Test
    void campingCardSupplyIsPaidByCard() {
        assertThat(PaymentChannel.of(false, true)).isEqualTo(PaymentChannel.CARD);
    }

    @Test
    void supplyWithoutCardIsPaidThroughTheApp() {
        assertThat(PaymentChannel.of(false, false)).isEqualTo(PaymentChannel.QR_APP);
    }

    @Test
    void houseCardIsNeverBilledAndOnlyRentalIsMarked() {
        assertThat(SettlementFlags.forHouseCard(true)).isEqualTo(new SettlementFlags(false, false, true));
        assertThat(SettlementFlags.forHouseCard(false)).isEqualTo(new SettlementFlags(false, false, false));
    }

    @Test
    void onlyNewRecordsAreExported() {
        for (ExportStatus status : ExportStatus.values()) {
            assertThat(status.isExportable()).isEqualTo(status == ExportStatus.NEW);
        }
    }

    @Test
    void onlySuspectRecordsWaitForReview() {
        for (RecordValidationState state : RecordValidationState.values()) {
            assertThat(state.isWaitingForReview()).isEqualTo(state == RecordValidationState.SUSPECT);
        }
    }

    @Test
    void rejectingARecordStopsItsExport() {
        ConsumptionRecord record = new ConsumptionRecord();
        record.markSuspect("too much");

        record.reject("admin", java.time.LocalDateTime.of(2026, 7, 12, 9, 0), "meter swap");

        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.IGNORED);
        assertThat(record.getReviewDate()).isEqualTo(java.time.LocalDateTime.of(2026, 7, 12, 9, 0));
    }
}
