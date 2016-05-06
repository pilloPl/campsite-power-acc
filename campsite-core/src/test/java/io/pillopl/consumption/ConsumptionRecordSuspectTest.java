package io.pillopl.consumption;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumptionRecordSuspectTest {

    @Test
    void suspectRecordKeepsItsReasonAndWaitsForReview() {
        ConsumptionRecord record = new ConsumptionRecord();

        record.markSuspect("Negative consumption: -2 kWh");

        assertThat(record.isSuspect()).isTrue();
        assertThat(record.getSuspectReason()).isEqualTo("Negative consumption: -2 kWh");
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
    }

    @Test
    void recordIsNotMarkedSuspectWithoutAReason() {
        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("BAY-20260712-000007");

        assertThatThrownBy(() -> record.markSuspect(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BAY-20260712-000007 needs a reason");
        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.VALID);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
    }
}
