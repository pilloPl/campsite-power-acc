package io.pillopl.consumption;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Finishes a new consumption record: gives it its natural id and decides whether its values are suspect.
 * A suspect record is the same record in the same table, waiting for a review.
 */
@Component
public class ConsumptionRecordFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordFactory.class);

    static final String UNKNOWN_CAMPSITE_CODE = "NA";

    private static final DateTimeFormatter RECORD_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SystemPropertyService systemPropertyService;

    private final ConsumptionRecordDao consumptionRecordDao;

    public ConsumptionRecordFactory(SystemPropertyService systemPropertyService,
                                    ConsumptionRecordDao consumptionRecordDao) {
        this.systemPropertyService = systemPropertyService;
        this.consumptionRecordDao = consumptionRecordDao;
    }

    /**
     * Sets the record number and the validation state of a record built from a finished session.
     *
     * @return true when the record is suspect
     */
    public boolean completeNewRecord(ConsumptionRecord record) {
        record.setRecordNo(generateRecordId(record.getCampsiteCode(), record.getPlugOut()));

        String suspectReason = suspectReasonFor(record.getDurationSeconds(), record.getKwh(), record.getNights());
        if (suspectReason != null) {
            record.markSuspect(suspectReason);
            LOGGER.info(String.format("Suspect consumption record to save (%s): %s", suspectReason, record));
            return true;
        }
        record.setValidationState(RecordValidationState.VALID);
        LOGGER.info(String.format("Consumption record to save: %s", record));
        return false;
    }

    public boolean isSuspect(Long durationSeconds, BigDecimal kwh, int nights) {
        return suspectReasonFor(durationSeconds, kwh, nights) != null;
    }

    private static String plain(BigDecimal kwh) {
        return kwh.stripTrailingZeros().toPlainString();
    }

    /**
     * Implausible values of a supply: negative duration, negative consumption, or more kWh per night of power use
     * than the configured threshold ({@code SuspectRecordKwhPerNightThreshold}). A supply within one day counts as
     * one night.
     *
     * @return the reason or null when the values are plausible
     */
    String suspectReasonFor(Long durationSeconds, BigDecimal kwh, int nights) {
        if (durationSeconds != null && durationSeconds < 0L) {
            return "Negative duration: " + durationSeconds + " s";
        }

        if (kwh != null && kwh.signum() < 0) {
            return "Negative consumption: " + plain(kwh) + " kWh";
        }

        BigDecimal threshold = systemPropertyService.getSuspectRecordKwhPerNightThreshold();
        int countedNights = Math.max(1, nights);
        if (kwh != null && threshold != null
                && kwh.compareTo(threshold.multiply(BigDecimal.valueOf(countedNights))) > 0) {
            return "Consumption " + plain(kwh) + " kWh in " + countedNights + " night(s) is above " + plain(threshold)
                    + " kWh per night";
        }

        return null;
    }

    /**
     * Natural id {@code <campsite code>-<yyyyMMdd of plug-out>-<number>}. The number comes from a sequence, is
     * padded to six digits and never cut: two records never share an id.
     */
    public String generateRecordId(String campsiteCode, LocalDateTime plugOut) {
        if (plugOut == null) {
            throw new IllegalArgumentException("Record id of campsite " + campsiteCode + " needs the plug-out");
        }
        String code = campsiteCode == null || campsiteCode.isBlank() ? UNKNOWN_CAMPSITE_CODE : campsiteCode.strip();
        return formatRecordId(code, plugOut, consumptionRecordDao.nextRecordNumber());
    }

    static String formatRecordId(String campsiteCode, LocalDateTime plugOut, long number) {
        return campsiteCode + "-" + RECORD_DAY.format(plugOut) + "-" + String.format("%06d", number);
    }
}
