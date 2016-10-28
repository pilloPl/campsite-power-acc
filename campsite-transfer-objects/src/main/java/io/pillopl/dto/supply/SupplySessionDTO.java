package io.pillopl.dto.supply;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A supply session as the console lists it: socket and pedestal, what started it (camping card, house card or QR
 * payment), the stay, the meter readings and how it ended. For an active session {@code stopMeterKwh} is the
 * socket's last reading and {@code kwh} the consumption so far; {@code stale} marks an active session whose socket
 * and pedestal have been silent for longer than StaleSessionTimeoutInHours.
 */
public record SupplySessionDTO(Long id, Integer transactionId, boolean active, boolean stale, Long socketId,
                               String socketCode, String socketName, String pitchNo, String pedestalCode,
                               Long campsiteGroupId, String campsiteName, String cardNumber, String cardRfid,
                               String houseCardType, Long qrPayProcessId, Long stayId, String guestName,
                               String unitPlate, int maxCurrentA, LocalDateTime start, LocalDateTime stop,
                               String stopReason, BigDecimal startMeterKwh, BigDecimal stopMeterKwh, BigDecimal kwh,
                               Long consumptionRecordId, LocalDateTime lastReading) implements Serializable {
}
