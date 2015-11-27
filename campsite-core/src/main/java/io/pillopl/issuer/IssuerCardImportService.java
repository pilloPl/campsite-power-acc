package io.pillopl.issuer;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CampingCardService;
import io.pillopl.card.CardActivation;
import io.pillopl.card.RfidConverter;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.attribute.WhitelistFilterService;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.issuer.IssuerExchange.CardLine;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Import of the card list changes an issuer partner sends: the partner's members get access to the pedestals of
 * the campsites that accept the partner's ISSUER group. Network cards (subtree of {@code NETWORK CARD}) are never
 * overwritten by an import; a card already imported from another partner is refused.
 * <p>
 * The cards of a partner hang on one guest named after the partner in the partner's ISSUER group. That guest has no
 * billing accounts: the partner bills its members itself, the card carries the partner's account number.
 */
@Service
public class IssuerCardImportService {

    private static final Logger LOG = LoggerFactory.getLogger(IssuerCardImportService.class);

    static final DateTimeFormatter ACTION_DATE_FORMAT = DateTimeFormatter.ofPattern(IssuerExchange.ACTION_DATE_PATTERN);

    private final CampingCardDao campingCardDao;

    private final CampingCardService campingCardService;

    private final GuestDao guestDao;

    private final GroupService groupService;

    private final WhitelistFilterService whitelistFilterService;

    private final DateService dateService;

    public IssuerCardImportService(CampingCardDao campingCardDao, CampingCardService campingCardService,
                                   GuestDao guestDao, GroupService groupService,
                                   WhitelistFilterService whitelistFilterService, DateService dateService) {
        this.campingCardDao = campingCardDao;
        this.campingCardService = campingCardService;
        this.guestDao = guestDao;
        this.groupService = groupService;
        this.whitelistFilterService = whitelistFilterService;
        this.dateService = dateService;
    }

    /**
     * Imports one card line of the partner.
     *
     * @return the card as it is now
     * @throws IssuerExchangeException when the line is refused
     */
    @Transactional
    public CampingCard importCard(final IssuerPartner partner, final CardLine cardLine) {
        checkWhitelistSynchronizable(partner);

        final String cardRfidHex = cardLine.cardRfidHex();
        if (cardRfidHex == null || cardRfidHex.isBlank()) {
            throw new IssuerExchangeException("Card UID is missing in the line of " + partner.getName());
        }
        final String cardRfid = toDecimalUid(cardRfidHex);
        final CampingCard card = campingCardDao.findByCardRfid(cardRfid);
        if (card != null && isNetworkCard(card)) {
            // the card exists as a network card: never taken over by an issuer
            throw new IssuerExchangeException(String.format("CARD_UID='%s' exists as a network card already",
                    cardRfidHex));
        }

        final String actionCode = cardLine.actionCode();
        CampingCard imported;
        if (IssuerExchange.ACTION_CODE_ADD.equalsIgnoreCase(actionCode)) {

            if (!CampingCard.isValidBillingAccountNo(cardLine.billingAccountNo())) {
                throw new IssuerExchangeException(String.format("CARD_UID='%s' billing account number '%s' is not valid",
                        cardRfidHex, cardLine.billingAccountNo()));
            }
            if (card != null && isIssuerCard(card)) {
                checkIssuer(partner, card, cardRfidHex);
                card.setActivation(CardActivation.ACTIVATED);
                card.setValidFrom(parseActionDate(cardLine));
                card.setValidTo(cardLine.validTo());
                card.setBillingAccountNo(cardLine.billingAccountNo());
                imported = campingCardDao.save(card);
            } else {
                Guest guest = getPartnerGuest(partner);
                imported = campingCardService.importCard(cardRfidHex, cardLine.cardNumber(),
                        parseActionDate(cardLine), cardLine.validTo(), cardLine.billingAccountNo(), guest.getId());
                imported.setCreateDate(dateService.getNow());
            }
        } else if (IssuerExchange.ACTION_CODE_MODIFY.equalsIgnoreCase(actionCode)) {
            // a card on its way out cannot be changed any more
            if (card != null && isIssuerCard(card)) {
                if (CardActivation.DEACTIVATED == card.getActivation()) {
                    throw new IssuerExchangeException(String.format("CARD_UID='%s' is being deleted", cardRfidHex));
                }
                checkIssuer(partner, card, cardRfidHex);
                card.setValidFrom(parseActionDate(cardLine));
                if (cardLine.cardNumber() != null && !cardLine.cardNumber().isBlank()) {
                    card.setCardNumber(cardLine.cardNumber());
                }
                card.setValidTo(cardLine.validTo());
                imported = campingCardDao.save(card);
            } else {
                throw new IssuerExchangeException(String.format("CARD_UID='%s' card not found", cardRfidHex));
            }
        } else if (IssuerExchange.ACTION_CODE_DELETE.equalsIgnoreCase(actionCode)) {
            if (card != null && isIssuerCard(card)) {
                checkIssuer(partner, card, cardRfidHex);
                card.setValidFrom(parseActionDate(cardLine));
                card.setValidTo(null);
                card.setActivation(CardActivation.DEACTIVATED);
                imported = campingCardDao.save(card);
            } else {
                throw new IssuerExchangeException(String.format("CARD_UID='%s' card not found", cardRfidHex));
            }
        } else {
            throw new IssuerExchangeException(String.format("CARD_UID='%s' unknown action code %s", cardRfidHex,
                    actionCode));
        }

        // cards of the issuers never go back into the card list export
        imported.setPartnerSyncState(PartnerSyncState.NEVER);
        imported.setTargetPartnerSyncState(PartnerSyncState.NEVER);
        LOG.info(String.format("Card %s of %s imported (%s)", imported, partner.getName(), actionCode));
        return campingCardDao.save(imported);
    }

    /**
     * The partner's whole ISSUER group may be switched off for the import by a whitelist filter.
     */
    private void checkWhitelistSynchronizable(IssuerPartner partner) {
        Group group = partner.getIssuerGroup();
        if (group != null) {
            boolean synchronizable = whitelistFilterService.isSynchronizableForImport(group.getId(), partner);
            if (!synchronizable) {
                throw new IssuerExchangeException(String.format("Issuer '%s' is marked as not synchronizable",
                        group.getDisplayName() != null ? group.getDisplayName() : group.getName()));
            }
        }
    }

    private void checkIssuer(IssuerPartner partner, CampingCard card, String cardRfidHex) {
        List<Long> partnerGroupIds = groupService.findManageable(partner.getIssuerGroup());
        if (!partnerGroupIds.contains(card.getGuest().getGroup().getId())) {
            throw new IssuerExchangeException(String.format("CARD_UID='%s' was imported from another issuer already",
                    cardRfidHex));
        }
    }

    /**
     * The guest that holds the partner's cards, created on the first import.
     */
    private Guest getPartnerGuest(IssuerPartner partner) {
        Group issuerGroup = partner.getIssuerGroup();
        Guest guest = guestDao.findByGroupIdAndName(issuerGroup.getId(), partner.getName());
        if (guest == null) {
            guest = new Guest(issuerGroup, GuestType.TRANSIT, partner.getName());
            guest.setOrganisationName(partner.getDisplayName());
            guest.setCreateDate(dateService.getNow());
            guest = guestDao.save(guest);
            LOG.info(String.format("Guest %s created for the cards of %s", guest.getId(), partner.getName()));
        }
        return guest;
    }

    /**
     * Is the card a network card, i.e. its guest hangs in the subtree of {@code NETWORK CARD}?
     */
    boolean isNetworkCard(CampingCard card) {
        if (card != null && card.getGuest() != null && card.getGuest().getGroup() != null) {
            Group networkCardGroup = groupService.fetchNetworkCardIssuerGroup();
            if (networkCardGroup != null) {
                List<Long> networkGroupIds = groupService.findManageable(networkCardGroup);
                return networkGroupIds.contains(card.getGuest().getGroup().getId());
            }
        }
        return false;
    }

    /**
     * Is the card a card of an issuer, i.e. its guest hangs in an ISSUER group outside the network card's subtree?
     */
    boolean isIssuerCard(CampingCard card) {
        if (card != null && card.getGuest() != null && card.getGuest().getGroup() != null) {
            Group networkCardGroup = groupService.fetchNetworkCardIssuerGroup();
            if (networkCardGroup != null) {
                List<Long> networkGroupIds = groupService.findManageable(networkCardGroup);
                return !networkGroupIds.contains(card.getGuest().getGroup().getId());
            }
            return true;
        }
        return false;
    }

    private static String toDecimalUid(String cardRfidHex) {
        try {
            return RfidConverter.hexBigEndianToDecimal(cardRfidHex.trim());
        } catch (RuntimeException e) {
            throw new IssuerExchangeException(String.format("CARD_UID='%s' is no hexadecimal UID", cardRfidHex), e);
        }
    }

    private static LocalDate parseActionDate(final CardLine cardLine) {
        LocalDate actionDate = null;
        try {
            if (cardLine.actionDate() != null && !cardLine.actionDate().isBlank()) {
                actionDate = LocalDateTime.parse(cardLine.actionDate().trim(), ACTION_DATE_FORMAT).toLocalDate();
            }
        } catch (DateTimeParseException e) {
            throw new IssuerExchangeException(String.format("CARD_UID='%s' cannot parse the action date %s",
                    cardLine.cardRfidHex(), cardLine.actionDate()));
        }
        return actionDate;
    }
}
