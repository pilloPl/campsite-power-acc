package io.pillopl.dto.consumption;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A consumption record as the console shows it: where and with which card the supply ran, the energy, the time, the
 * price and where the record stands in validation and export.
 */
public record ConsumptionRecordDTO(Long id, String recordNo, String pedestalCode, String socketCode, String pitchNo,
                                   String campsiteCode, String campsiteName, String siteTenant, String issuerTenant,
                                   String cardRfid, String cardNumber, String guestName, String paymentChannel,
                                   boolean billable, boolean settledByCampsite, boolean houseCardRental,
                                   LocalDateTime plugIn, LocalDateTime plugOut, Long durationSeconds, int nights,
                                   BigDecimal kwh, String stopReason, BigDecimal cost, String currency,
                                   String pricingStatus, String pricingFailureReason, String tariffName,
                                   String validationState, String suspectReason, String exportStatus,
                                   String reviewedBy, LocalDateTime reviewDate) implements Serializable {
}
