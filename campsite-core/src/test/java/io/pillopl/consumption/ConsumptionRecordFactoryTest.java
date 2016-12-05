package io.pillopl.consumption;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumptionRecordFactoryTest {

    static final LocalDateTime PLUG_OUT = LocalDateTime.of(2026, 7, 14, 10, 15);

    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    final ConsumptionRecordDao consumptionRecordDao = mock(ConsumptionRecordDao.class);
    final ConsumptionRecordFactory factory = new ConsumptionRecordFactory(systemPropertyService,
            consumptionRecordDao);

    @BeforeEach
    void setUp() {
        when(systemPropertyService.getSuspectRecordKwhPerNightThreshold()).thenReturn(BigDecimal.valueOf(300));
        when(consumptionRecordDao.nextRecordNumber()).thenReturn(42L);
    }

    @Test
    void negativeDurationIsSuspect() {
        assertThat(factory.suspectReasonFor(-60L, new BigDecimal("3.5"), 1)).startsWith("Negative duration");
    }

    @Test
    void negativeConsumptionIsSuspect() {
        assertThat(factory.suspectReasonFor(3600L, new BigDecimal("-0.5"), 1)).startsWith("Negative consumption");
    }

    @Test
    void negativeDurationIsTheReasonWhenTheConsumptionIsNegativeToo() {
        assertThat(factory.suspectReasonFor(-60L, new BigDecimal("-0.5"), 1)).isEqualTo("Negative duration: -60 s");
    }

    @Test
    void moreThanTheThresholdPerNightIsSuspect() {
        assertThat(factory.isSuspect(3 * 86400L, new BigDecimal("900.0001"), 3)).isTrue();
    }

    @Test
    void exactlyTheThresholdPerNightIsPlausible() {
        assertThat(factory.isSuspect(3 * 86400L, new BigDecimal("900"), 3)).isFalse();
    }

    @Test
    void supplyWithinOneDayCountsAsOneNight() {
        assertThat(factory.isSuspect(7200L, new BigDecimal("301"), 0)).isTrue();
        assertThat(factory.isSuspect(7200L, new BigDecimal("12"), 0)).isFalse();
    }

    @Test
    void thresholdComesFromTheConfiguration() {
        when(systemPropertyService.getSuspectRecordKwhPerNightThreshold()).thenReturn(BigDecimal.valueOf(50));

        assertThat(factory.isSuspect(86400L, new BigDecimal("51"), 1)).isTrue();
    }

    @Test
    void unknownConsumptionIsNotSuspect() {
        assertThat(factory.isSuspect(3600L, null, 1)).isFalse();
    }

    @Test
    void recordIdIsCampsiteCodeDayOfPlugOutAndNumber() {
        assertThat(factory.generateRecordId("BAY", PLUG_OUT)).isEqualTo("BAY-20260714-000042");
    }

    @Test
    void recordIdOfAnUnknownCampsiteUsesAPlaceholderCode() {
        assertThat(factory.generateRecordId(" ", PLUG_OUT)).isEqualTo("NA-20260714-000042");
    }

    @Test
    void recordIdWithoutPlugOutTakesNoNumber() {
        assertThatThrownBy(() -> factory.generateRecordId("BAY", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("needs the plug-out");
        verify(consumptionRecordDao, never()).nextRecordNumber();
    }

    @Test
    void largeNumbersAreNeverCutSoRecordIdsStayUnique() {
        assertThat(ConsumptionRecordFactory.formatRecordId("BAY", PLUG_OUT, 12_345_678_901L))
                .isEqualTo("BAY-20260714-12345678901");
        assertThat(ConsumptionRecordFactory.formatRecordId("BAY", PLUG_OUT, 1_000_001L))
                .isNotEqualTo(ConsumptionRecordFactory.formatRecordId("BAY", PLUG_OUT, 1L));
    }

    @Test
    void suspectRecordWaitsForReviewAndIsNotExported() {
        ConsumptionRecord record = record(new BigDecimal("700"), 86400L, 1);

        assertThat(factory.completeNewRecord(record)).isTrue();

        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.SUSPECT);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
        assertThat(record.getSuspectReason()).contains("700 kWh");
        assertThat(record.getRecordNo()).isEqualTo("BAY-20260714-000042");
    }

    @Test
    void plausibleRecordIsValidAndWaitsForExport() {
        ConsumptionRecord record = record(new BigDecimal("14.2"), 86400L, 1);

        assertThat(factory.completeNewRecord(record)).isFalse();

        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.VALID);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
    }

    private static ConsumptionRecord record(BigDecimal kwh, Long durationSeconds, int nights) {
        ConsumptionRecord record = new ConsumptionRecord();
        record.setCampsiteCode("BAY");
        record.setPlugOut(PLUG_OUT);
        record.setKwh(kwh);
        record.setDurationSeconds(durationSeconds);
        record.setNights(nights);
        return record;
    }
}
