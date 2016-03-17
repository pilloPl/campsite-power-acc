package io.pillopl.issuer;

import io.pillopl.dto.consumption.ConsumptionRecordDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Transport of the exchange with an issuer partner, one implementation per interface generation
 * ({@link IssuerPartner.InterfaceVersion}). The transport only moves lines; what is imported and exported is decided
 * by {@link IssuerExchangeService}, {@link IssuerCardImportService} and {@link IssuerExchangeImportService}.
 *
 * @throws IssuerExchangeException from every method when the partner cannot be reached or its data cannot be read
 */
public interface IssuerExchange {

    String ACTION_CODE_ADD = "ADD";

    String ACTION_CODE_MODIFY = "MODIFY";

    String ACTION_CODE_DELETE = "DELETE";

    /**
     * Format of the action date of a card line.
     */
    String ACTION_DATE_PATTERN = "yyyy-MM-dd HH:mm";

    /**
     * One card of a card list: the UID as big-endian hex (as printed by the card producers), the printed number,
     * the action (ADD, MODIFY, DELETE) from the action date on, the billing account and the end of the validity.
     */
    record CardLine(String cardRfidHex, String cardNumber, String actionCode, String actionDate,
                    String billingAccountNo, LocalDate validTo) {
    }

    /**
     * Outcome of one direction of a run: accepted and rejected lines, the first rejections as message.
     */
    record ExchangeResult(int accepted, int rejected, String message) {

        public static ExchangeResult sent(int count) {
            return new ExchangeResult(count, 0, null);
        }
    }

    IssuerPartner.InterfaceVersion getInterfaceVersion();

    /**
     * The card list changes the partner has for the network since the last fetch.
     */
    List<CardLine> fetchCards(IssuerPartner partner);

    /**
     * The changes of the network's card list for the partner's campsites.
     */
    void sendCards(IssuerPartner partner, List<CardLine> cards);

    /**
     * The consumption records of the partner's card holders at the network's campsites.
     */
    void sendConsumptionRecords(IssuerPartner partner, List<ConsumptionRecordDTO> records);

    /**
     * The consumption records of network card holders at the partner's campsites since the last fetch.
     */
    List<ConsumptionRecordDTO> fetchConsumptionRecords(IssuerPartner partner);
}
