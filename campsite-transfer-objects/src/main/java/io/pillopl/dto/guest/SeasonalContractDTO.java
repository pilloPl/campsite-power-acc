package io.pillopl.dto.guest;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A seasonal contract: account, tariff, pitch, period, fixed fee and the kWh packages included.
 */
public record SeasonalContractDTO(Long id, Long guestId, String guestName, Long billingAccountId,
                                  String billingAccountNo, Long tariffId, String tariffName, String status,
                                  Long siteGroupId, String campsiteName, String pitchNo, LocalDate startDate,
                                  LocalDate endDate, LocalDate requestedEndDate, BigDecimal fixedFee,
                                  String feePeriod, BigDecimal packageKwh) implements Serializable {
}
