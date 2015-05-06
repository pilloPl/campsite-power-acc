package io.pillopl.dto.qrpay;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A QR payment as the payment app shows it: the state of the current row, the supply of the socket and its cost
 * (the estimate while supplying, the price once the supply is stopped and priced).
 */
public record QrPayProcessDTO(Long processId, String orderReference, String socketCode, String state,
                              LocalDateTime requestDate, String priceInformation, Integer transactionId,
                              LocalDateTime supplyStart, LocalDateTime supplyStop, BigDecimal kwh,
                              Long durationSeconds, BigDecimal cost, BigDecimal preliminaryCost, String currency,
                              String errorCode, String errorText) implements Serializable {
}
