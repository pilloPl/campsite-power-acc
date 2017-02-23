package io.pillopl.card;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.issuer.PartnerSyncState;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoService;
import io.pillopl.util.DateService;
import io.pillopl.value.SocketLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Camping cards at the reception: issue a card for a stay or a season, block and unblock it, change the validity,
 * look cards up within the ISSUER scope of the user, and the upsert used by the card imports of the issuers.
 */
@Service
public class CampingCardService {

    private static final Logger LOG = LoggerFactory.getLogger(CampingCardService.class);

    private final CampingCardDao campingCardDao;

    private final GuestDao guestDao;

    private final UserinfoService userinfoService;

    private final ValidationService validationService;

    private final DateService dateService;

    private final GroupService groupService;

    public CampingCardService(CampingCardDao campingCardDao, GuestDao guestDao, UserinfoService userinfoService,
                              ValidationService validationService, DateService dateService,
                              GroupService groupService) {
        this.campingCardDao = campingCardDao;
        this.guestDao = guestDao;
        this.userinfoService = userinfoService;
        this.validationService = validationService;
        this.dateService = dateService;
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public CampingCard findCard(Long cardId) {
        return cardId != null ? campingCardDao.findById(cardId).orElse(null) : null;
    }

    @Transactional(readOnly = true)
    public CampingCard findByRfid(String cardRfid) {
        return cardRfid != null ? campingCardDao.findByCardRfid(cardRfid) : null;
    }

    /**
     * Living cards with the UID or the printed number whose guests the user manages.
     */
    @Transactional(readOnly = true)
    public List<CampingCard> findCards(Userinfo principal, String rfidOrNumber) {
        Set<CampingCard> found = new LinkedHashSet<>();
        if (rfidOrNumber != null && !rfidOrNumber.isBlank()) {
            String search = rfidOrNumber.trim();
            found.addAll(campingCardDao.findNotDeletedByCardRfid(search));
            found.addAll(campingCardDao.findNotDeletedByCardNumber(search));
        }
        List<CampingCard> result = new ArrayList<>();
        for (CampingCard card : found) {
            if (isManageable(principal, card)) {
                result.add(card);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CampingCard> findCardsOfGuest(Userinfo principal, Long guestId) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        return campingCardDao.findByGuestId(guestId);
    }

    /**
     * Issues a card at the reception: valid from arrival to departure (stay) or for the season.
     */
    @Transactional
    public CampingCard issueCard(Userinfo principal, Long guestId, String cardRfid, String cardNumber,
                                 LocalDate validFrom, LocalDate validTo, String billingAccountNo) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        if (cardRfid == null || !RfidConverter.isDecimalUid(cardRfid.trim())) {
            throw new IllegalArgumentException("Card UID must be the decimal UID of the reader: " + cardRfid);
        }
        checkValidity(validFrom, validTo);
        if (!CampingCard.isValidBillingAccountNo(billingAccountNo)) {
            throw new IllegalArgumentException("Billing account number is not valid: " + billingAccountNo);
        }
        String rfid = cardRfid.trim();
        for (CampingCard existing : campingCardDao.findNotDeletedByCardRfid(rfid)) {
            if (existing.getCardNumber().equals(cardNumber)) {
                throw new IllegalStateException(String.format("Card %s / %s is already issued", rfid, cardNumber));
            }
        }
        CampingCard card = new CampingCard.Builder(rfid, cardNumber)
                .guest(guest)
                .validFrom(validFrom)
                .validTo(validTo)
                .billingAccountNo(billingAccountNo)
                .build();
        card.setCreateDate(dateService.getNow());
        card = campingCardDao.save(card);
        LOG.info(String.format("Card %s issued to %s by %s", card, guest, principal.getUsername()));
        return card;
    }

    @Transactional
    public CampingCard changeActivation(Userinfo principal, Long cardId, CardActivation activation) {
        CampingCard card = getCard(cardId);
        checkIsManageable(principal, card);
        card.setActivation(activation);
        // the issuer partners learn about a blocked card (DELETE) or an unblocked one (ADD)
        card.calculatePartnerSyncState(activation == CardActivation.DEACTIVATED ? PartnerSyncState.DELETE
                : PartnerSyncState.ADD);
        return campingCardDao.save(card);
    }

    /**
     * The card goes to the issuer partners again with the next card export: a blocked card as a delete, any other as
     * a change (an add when the partners never got it).
     *
     * @throws IllegalStateException when the card is kept out of the exchange for good, or the partners never got a
     *                               card that is blocked (nothing to send)
     */
    @Transactional
    public CampingCard requestPartnerResync(Userinfo principal, Long cardId) {
        CampingCard card = getCard(cardId);
        checkIsManageable(principal, card);
        if (card.getPartnerSyncState() == PartnerSyncState.NEVER) {
            throw new IllegalStateException("Card " + card.getCardNumber() + " is kept out of the partner exchange");
        }
        card.calculatePartnerSyncState(card.getActivation() == CardActivation.DEACTIVATED ? PartnerSyncState.DELETE
                : PartnerSyncState.MODIFY);
        if (!card.getPartnerSyncState().isExported()) {
            throw new IllegalStateException("Card " + card.getCardNumber() + " has nothing to send to the partners ("
                    + card.getPartnerSyncState() + ")");
        }
        return campingCardDao.save(card);
    }

    /**
     * Unassigned cards of the welcome card pools of the markets, as far as the user manages the pool.
     */
    @Transactional(readOnly = true)
    public List<CampingCard> findWelcomePoolCards(Userinfo principal) {
        List<CampingCard> result = new ArrayList<>();
        for (Group pool : welcomeCardPools()) {
            for (CampingCard card : campingCardDao.findNotDeletedByGuestGroupId(pool.getId())) {
                if (card.getActivation() == CardActivation.ACTIVATED && isManageable(principal, card)) {
                    result.add(card);
                }
            }
        }
        return result;
    }

    /**
     * A card of the welcome card pool is handed to a guest at the reception: it leaves the pool, belongs to the guest
     * (and so to the guest's card issuer) for the given validity, bills to the guest's open billing account, and goes
     * to the issuer partners as a new card.
     *
     * @throws IllegalStateException when the card is not an unassigned card of a welcome card pool, or the guest is a
     *                               pool guest himself
     */
    @Transactional
    public CampingCard assignWelcomeCard(Userinfo principal, Long cardId, Long guestId, LocalDate validFrom,
                                         LocalDate validTo) {
        CampingCard card = getCard(cardId);
        checkIsManageable(principal, card);
        if (!isWelcomePoolCard(card) || card.getActivation() != CardActivation.ACTIVATED) {
            throw new IllegalStateException("Card " + card.getCardNumber() + " is not a free card of a welcome card "
                    + "pool");
        }
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        if (isWelcomePool(guest.getGroup())) {
            throw new IllegalStateException("Guest " + guest.getId() + " belongs to a welcome card pool");
        }
        LocalDate from = validFrom != null ? validFrom : dateService.getToday();
        if (validTo == null) {
            throw new IllegalArgumentException("The last day of the card's validity is missing");
        }
        checkValidity(from, validTo);
        // the card bills to the guest's open account; guests billed by their issuer keep the card's account
        String account = guest.getBillingAccounts().stream().filter(BillingAccount::isBillable)
                .map(BillingAccount::getErpAccountNo).findFirst().orElse(null);
        if (account == null && !guest.getBillingAccounts().isEmpty()) {
            throw new IllegalStateException("Guest " + guest.getId() + " has no open billing account");
        }
        Guest pool = card.getGuest();
        card.setGuest(guest);
        if (account != null) {
            card.setBillingAccountNo(account);
        }
        card.setValidFrom(from);
        card.setValidTo(validTo);
        card.calculatePartnerSyncState(PartnerSyncState.ADD);
        card = campingCardDao.save(card);
        LOG.info(String.format("Welcome card %s of pool guest %s assigned to %s by %s", card, pool.getId(), guest,
                principal.getUsername()));
        return card;
    }

    boolean isWelcomePoolCard(CampingCard card) {
        return card.getGuest() != null && isWelcomePool(card.getGuest().getGroup());
    }

    private boolean isWelcomePool(Group group) {
        return group != null && welcomeCardPools().stream().anyMatch(pool -> pool.getId().equals(group.getId()));
    }

    private List<Group> welcomeCardPools() {
        List<Group> pools = new ArrayList<>();
        for (Market market : Market.values()) {
            Group pool = groupService.getExtensionByMarket(market).findWelcomeCardPoolIssuerGroup();
            if (pool != null) {
                pools.add(pool);
            }
        }
        return pools;
    }

    /**
     * The stay was extended or shortened.
     */
    @Transactional
    public CampingCard changeValidity(Userinfo principal, Long cardId, LocalDate validFrom, LocalDate validTo) {
        CampingCard card = getCard(cardId);
        checkIsManageable(principal, card);
        checkValidity(validFrom, validTo);
        card.setValidFrom(validFrom);
        card.setValidTo(validTo);
        card.calculatePartnerSyncState(PartnerSyncState.MODIFY);
        return campingCardDao.save(card);
    }

    /**
     * Dry run of the pedestal's question: what would the pedestal answer to the card now (nothing is started).
     */
    @Transactional(readOnly = true)
    public ValidationService.ValidationResult checkCardAtSocket(Userinfo principal, String cardRfid,
                                                               SocketLinkId socketLinkId) {
        CampingCard card = findByRfid(cardRfid);
        if (card != null) {
            checkIsManageable(principal, card);
        }
        return validationService.validateForNow(socketLinkId, cardRfid);
    }

    /**
     * Card of an issuer file (UID as big-endian hex), inserted or updated through CARD_UTILS.UPSERT_CARD.
     */
    @Transactional
    public CampingCard importCard(String cardRfidHex, String cardNumber, LocalDate validFrom, LocalDate validTo,
                                  String billingAccountNo, Long guestId) {
        checkValidity(validFrom, validTo);
        Long id = campingCardDao.upsertCard(cardRfidHex, cardNumber, validFrom, validTo, billingAccountNo, guestId);
        return campingCardDao.findById(id).orElseThrow();
    }

    public boolean isManageable(Userinfo principal, CampingCard card) {
        return card.getGuest() != null && userinfoService.isManageable(principal, card.getGuest());
    }

    private void checkIsManageable(Userinfo principal, CampingCard card) {
        if (!isManageable(principal, card)) {
            throw new AccessDeniedException(String.format("User '%s' has no access to card %s",
                    principal.getUsername(), card.getId()));
        }
    }

    private void checkIsManageable(Userinfo principal, Guest guest) {
        if (!userinfoService.isManageable(principal, guest)) {
            throw new AccessDeniedException(String.format("User '%s' has no access to guest %s",
                    principal.getUsername(), guest.getId()));
        }
    }

    private static void checkValidity(LocalDate validFrom, LocalDate validTo) {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            throw new IllegalArgumentException("Validity ends before it starts: " + validFrom + " - " + validTo);
        }
    }

    private Guest getGuest(Long guestId) {
        Guest guest = guestId != null ? guestDao.findById(guestId).orElse(null) : null;
        if (guest == null || guest.isDeleted()) {
            throw new NoSuchElementException("Guest " + guestId + " not found");
        }
        return guest;
    }

    private CampingCard getCard(Long cardId) {
        CampingCard card = findCard(cardId);
        if (card == null || card.isDeleted()) {
            throw new NoSuchElementException("Card " + cardId + " not found");
        }
        return card;
    }
}
