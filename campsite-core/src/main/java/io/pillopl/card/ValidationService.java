package io.pillopl.card;

import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.util.DateService;
import io.pillopl.value.QrPayAuthTag;
import io.pillopl.value.SocketLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Validation of a card at a socket or a pedestal: the decision whether the pedestal may supply with this card.
 */
@Service
public class ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);

    /**
     * Outcome of a validation: the status and what was found on the way (socket, card, house card, guest).
     */
    public static class ValidationResult {

        private ValidationStatus status;

        private CampingCard card;

        private HouseCard houseCard;

        private Guest guest;

        private Socket socket;

        private boolean checkStatus;

        private boolean freeSocket;

        private Long qrPayProcessId;

        public ValidationResult() {
        }

        public ValidationResult(Builder builder) {
            this.status = builder.validationStatus;
            this.guest = builder.guest;
            this.card = builder.card;
            this.houseCard = builder.houseCard;
            this.socket = builder.socket;
            this.checkStatus = builder.checkStatus;
        }

        public boolean isHouseCardAccepted() {
            return houseCard != null;
        }

        public ValidationStatus getStatus() {
            return status;
        }

        public void setStatus(final ValidationStatus status) {
            this.status = status;
        }

        public CampingCard getCard() {
            return card;
        }

        public void setCard(final CampingCard card) {
            this.card = card;
        }

        public HouseCard getHouseCard() {
            return houseCard;
        }

        public void setHouseCard(HouseCard houseCard) {
            this.houseCard = houseCard;
        }

        public Guest getGuest() {
            return guest;
        }

        public void setGuest(Guest guest) {
            this.guest = guest;
        }

        public Socket getSocket() {
            return socket;
        }

        public void setSocket(final Socket socket) {
            this.socket = socket;
        }

        /**
         * True when the card was only checked for a guest (supply already started by the pedestal), not validated.
         */
        public boolean isCheckStatus() {
            return checkStatus;
        }

        public void setCheckStatus(boolean checkStatus) {
            this.checkStatus = checkStatus;
        }

        /**
         * True when the socket supplies as free socket: no card needed, nothing billed.
         */
        public boolean isFreeSocket() {
            return freeSocket;
        }

        public void setFreeSocket(boolean freeSocket) {
            this.freeSocket = freeSocket;
        }

        /**
         * The auth tag was the remote start of this QR payment process (no card, no house card).
         */
        public Long getQrPayProcessId() {
            return qrPayProcessId;
        }

        public void setQrPayProcessId(Long qrPayProcessId) {
            this.qrPayProcessId = qrPayProcessId;
        }

        public boolean isQrPayAccepted() {
            return qrPayProcessId != null && status == ValidationStatus.ACCEPTED;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("ValidationResult[status=").append(status).append(";socket=")
                    .append(socket == null ? "null" : socket.getSocketCode()).append(";checkStatus=")
                    .append(checkStatus).append(";card=").append(card == null ? "null" : card.getCardRfid())
                    .append(";houseCard=").append(houseCard == null ? "null" : houseCard.getRfid())
                    .append(";guest=").append(guest == null ? "null" : guest.getFullName())
                    .append(";freeSocket=").append(freeSocket).append(";qrPayProcess=").append(qrPayProcessId).append("]").toString();
        }

        public static class Builder {

            private boolean checkStatus;

            private Socket socket;

            private CampingCard card;

            private HouseCard houseCard;

            private Guest guest;

            private ValidationStatus validationStatus;

            public Builder validationStatus(final ValidationStatus validationStatus) {
                Objects.requireNonNull(validationStatus, "validationStatus");
                this.validationStatus = validationStatus;
                return this;
            }

            public Builder checkStatus(final boolean checkStatus) {
                this.checkStatus = checkStatus;
                return this;
            }

            public Builder socket(final Socket socket) {
                this.socket = socket;
                return this;
            }

            public Builder guest(final Guest guest) {
                this.guest = guest;
                return this;
            }

            public Builder card(final CampingCard card) {
                this.card = card;
                return this;
            }

            public Builder houseCard(final HouseCard houseCard) {
                this.houseCard = houseCard;
                return this;
            }

            public ValidationResult build() {
                return new ValidationResult(this);
            }
        }
    }

    private final PedestalService pedestalService;

    private final CampingCardDao campingCardDao;

    private final DateService dateService;

    private final GroupService groupService;

    private final HouseCardService houseCardService;

    private final ObjectProvider<QrPaySupplyAuthorizer> qrPaySupplyAuthorizer;

    public ValidationService(PedestalService pedestalService, CampingCardDao campingCardDao, DateService dateService,
                             GroupService groupService, HouseCardService houseCardService,
                             ObjectProvider<QrPaySupplyAuthorizer> qrPaySupplyAuthorizer) {
        this.pedestalService = pedestalService;
        this.campingCardDao = campingCardDao;
        this.dateService = dateService;
        this.groupService = groupService;
        this.houseCardService = houseCardService;
        this.qrPaySupplyAuthorizer = qrPaySupplyAuthorizer;
    }

    @Transactional(readOnly = true)
    public ValidationResult validateForNow(final SocketLinkId socketLinkId, final String cardRfid) {
        return validateFor(socketLinkId, cardRfid, dateService.getNow(), false);
    }

    @Transactional(readOnly = true)
    public ValidationResult validateForNow(final String pedestalCode, final String cardRfid) {
        return validateFor(pedestalCode, null, cardRfid, dateService.getNow(), false);
    }

    /**
     * @param remoteDate            the time the pedestal reported (the card must be valid then)
     * @param withoutCardValidation the pedestal already started the supply: only check that the card has a guest
     *                              to bill, do not validate it
     */
    @Transactional(readOnly = true)
    public ValidationResult validateFor(final SocketLinkId socketLinkId, final String cardRfid,
                                        final LocalDateTime remoteDate, final boolean withoutCardValidation) {
        final ValidationResult validationResult;
        final Socket socket = getSocket(socketLinkId);
        if (socket != null) {
            validationResult = validateFor(socketLinkId.getPedestalCode(), socket, cardRfid, remoteDate,
                    withoutCardValidation);
            validationResult.setSocket(socket);
            validationResult.setCheckStatus(withoutCardValidation);
        } else {
            validationResult = new ValidationResult();
            validationResult.setStatus(ValidationStatus.SOCKET_INVALID);
            validationResult.setCheckStatus(withoutCardValidation);
        }
        LOG.debug("Validation result: {}", validationResult);
        return validationResult;
    }

    private ValidationResult validateFor(final String pedestalCode, final Socket socket, final String cardRfid,
                                         final LocalDateTime remoteDate, final boolean withoutCardValidation) {
        final ValidationResult validationResult = new ValidationResult();
        validationResult.setCheckStatus(withoutCardValidation);
        // Check first if the pedestal exists
        final Pedestal pedestal = getPedestal(pedestalCode);
        if (pedestal == null) {
            validationResult.setStatus(ValidationStatus.SOCKET_INVALID);
            LOG.debug("Validation result: {}", validationResult);
            return validationResult;
        }

        // Is it a house card of the campsite?
        HouseCard houseCard = houseCardService.getHouseCardForPedestal(pedestalCode, cardRfid);
        if (houseCard != null) {
            validationResult.setHouseCard(houseCard);
            validationResult.setStatus(ValidationStatus.ACCEPTED);
            LOG.info(String.format("House card used. Returning result (%s)", validationResult));
            return validationResult;
        }

        // Is it the remote start of a QR payment?
        Long qrPayProcessId = QrPayAuthTag.parse(cardRfid);
        if (qrPayProcessId != null) {
            QrPaySupplyAuthorizer authorizer = qrPaySupplyAuthorizer.getIfAvailable();
            if (authorizer != null && socket != null
                    && authorizer.isSupplyAuthorized(qrPayProcessId, socket, withoutCardValidation)) {
                validationResult.setQrPayProcessId(qrPayProcessId);
                validationResult.setStatus(ValidationStatus.ACCEPTED);
                LOG.info(String.format("QR payment %s used. Returning result (%s)", qrPayProcessId,
                        validationResult));
            } else {
                validationResult.setStatus(ValidationStatus.CARD_INVALID);
                LOG.warn(String.format("QR payment auth tag [%s] not valid at socket %s. Returning result (%s)",
                        cardRfid, socket, validationResult));
            }
            return validationResult;
        }

        // No, then it must be a camping card
        CampingCard card = cardRfid != null ? campingCardDao.findByCardRfid(cardRfid) : null;
        if (card == null) {
            // the network does not know the card
            validationResult.setStatus(ValidationStatus.CARD_INVALID);
            LOG.warn(String.format("Card with UID [%s] is unknown (sent by pedestal: %s). Returning result (%s)",
                    cardRfid, pedestal, validationResult));
            return validationResult;
        }
        // the network knows the card
        validationResult.setCard(card);
        validationResult.setGuest(card.getGuest());

        // Must the card be validated?
        if (withoutCardValidation) {
            // No: the supply runs already, the card needs at least a guest to bill the consumption later
            if (card.getGuest() == null) {
                validationResult.setStatus(ValidationStatus.CARD_INVALID);
            } else {
                validationResult.setStatus(ValidationStatus.ACCEPTED);
            }
            validationResult.setFreeSocket(determineFreeSocket(socket));
            LOG.debug("Validation result: {}", validationResult);

            // enough validation for now
            return validationResult;
        }

        // Is the card accepted on this campsite?
        if (!isCardAcceptedAt(card, pedestal)) {
            validationResult.setStatus(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE);
            LOG.debug("Validation result: {}", validationResult);
            return validationResult;
        }

        // Is the card valid at the time the pedestal reported?
        CardStatus cardStatus = card.validate(remoteDate);
        validationResult.setStatus(translate(cardStatus));
        LOG.debug("Validation result: {}", validationResult);
        return validationResult;
    }

    /**
     * A free socket (flag of the socket, set in the console) supplies without a card; such supply is never billed.
     */
    boolean determineFreeSocket(Socket socket) {
        if (socket == null) {
            return false;
        }
        if (socket.isFreeSocket()) {
            LOG.info(String.format("Supply on free socket %s", socket));
            return true;
        }
        return false;
    }

    static ValidationStatus translate(CardStatus cardStatus) {
        ValidationStatus validationStatus;
        switch (cardStatus) {
            case NOT_VALID_ANYMORE:
            case NOT_VALID_YET:
                validationStatus = ValidationStatus.EXPIRED;
                break;
            case CARD_DEACTIVATED:
            case GUEST_DEACTIVATED:
                validationStatus = ValidationStatus.BLOCKED;
                break;
            case NO_GUEST:
            case BILLING_ACCOUNT_INVALID:
                validationStatus = ValidationStatus.CARD_INVALID;
                break;
            case VALID:
                validationStatus = ValidationStatus.ACCEPTED;
                break;
            default:
                validationStatus = ValidationStatus.UNAVAILABLE;
                break;
        }
        return validationStatus;
    }

    private boolean isCardAcceptedAt(CampingCard card, Pedestal pedestal) {
        return groupService.isCardAcceptedAt(pedestal.getId(), card);
    }

    private Pedestal getPedestal(String pedestalCode) {
        Pedestal pedestal = pedestalCode != null ? pedestalService.findByCode(pedestalCode) : null;
        return pedestal != null && !pedestal.isDeleted() ? pedestal : null;
    }

    private Socket getSocket(SocketLinkId socketLinkId) {
        if (socketLinkId == null) {
            return null;
        }
        return pedestalService.findSocket(socketLinkId.getPedestalCode(), socketLinkId.socketNo());
    }
}
