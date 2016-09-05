package io.pillopl.dto.qrpay;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A QR payment in the console list: the current state since when, where it supplies, the cost once priced and the
 * error. Stuck: unfinished and in its state longer than QrPayStuckAfterMinutes (the console may cancel it).
 */
public record QrPayProcessOverviewDTO(Long processId, String orderReference, String state, String socketCode,
                                      String pedestalCode, Long campsiteGroupId, String campsiteName,
                                      String clientUsername, String appUserId, LocalDateTime requestDate,
                                      LocalDateTime stateSince, Integer transactionId, BigDecimal kwh,
                                      BigDecimal cost, String currency, String errorCode, String errorMessage,
                                      boolean archived, boolean stuck) implements Serializable {
}
