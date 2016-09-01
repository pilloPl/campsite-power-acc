package io.pillopl.guest;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardActivation;
import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.BillingAccountDao;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.persistence.dao.SeasonalContractDao;
import io.pillopl.persistence.dao.StayDao;
import io.pillopl.supply.CheckedOutStayFinder;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoService;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Guests at the reception: guests and their details, units and billing accounts, and the stays with check-in and
 * check-out. The stay drives the validity of the card handed out for it; its check-out lets the stale-session job
 * close the supply sessions the pedestals left open.
 * <p>
 * The guest must lie in the user's ISSUER scope, the campsite of a stay in the user's SITE scope.
 */
@Primary
@Service
public class GuestService implements CheckedOutStayFinder {

    private static final Logger LOG = LoggerFactory.getLogger(GuestService.class);

    public static final String STAYS_ALL = "ALL";

    public static final String STAYS_OPEN = "OPEN";

    public static final String STAYS_CHECKED_OUT = "CHECKED_OUT";

    /**
     * Stands for "no group" in an IN list (an empty list is no valid IN list).
     */
    private static final List<Long> NO_GROUPS = List.of(-1L);

    private final GuestDao guestDao;

    private final StayDao stayDao;

    private final BillingAccountDao billingAccountDao;

    private final CampingCardDao campingCardDao;

    private final GroupService groupService;

    private final UserinfoService userinfoService;

    private final DateService dateService;

    private final SeasonalContractDao seasonalContractDao;

    public GuestService(GuestDao guestDao, StayDao stayDao, BillingAccountDao billingAccountDao,
                        CampingCardDao campingCardDao, GroupService groupService, UserinfoService userinfoService,
                        DateService dateService, SeasonalContractDao seasonalContractDao) {
        this.seasonalContractDao = seasonalContractDao;
        this.guestDao = guestDao;
        this.stayDao = stayDao;
        this.billingAccountDao = billingAccountDao;
        this.campingCardDao = campingCardDao;
        this.groupService = groupService;
        this.userinfoService = userinfoService;
        this.dateService = dateService;
    }

    @Transactional(readOnly = true)
    public Guest findGuest(Long guestId) {
        return guestId != null ? guestDao.findById(guestId).orElse(null) : null;
    }

    /**
     * Living guests of the user's ISSUER scope whose name, first name or plate contains the text (all for no text).
     */
    @Transactional(readOnly = true)
    public List<Guest> findGuests(Userinfo principal, String search) {
        Group issuerGroup = principal.getGroup(GroupType.ISSUER);
        if (issuerGroup == null) {
            LOG.info(String.format("User '%s' has no ISSUER scope, no guests", principal.getUsername()));
            return List.of();
        }
        List<Guest> guests = guestDao.findByGroupIds(groupService.findManageable(issuerGroup));
        if (search == null || search.isBlank()) {
            return guests;
        }
        String text = search.trim().toLowerCase(Locale.ROOT);
        Set<Long> cardHolders = new HashSet<>();
        for (CampingCard card : campingCardDao.findNotDeletedByCardNumber(search.trim())) {
            if (card.getGuest() != null) {
                cardHolders.add(card.getGuest().getId());
            }
        }
        List<Guest> found = new ArrayList<>();
        for (Guest guest : guests) {
            if (contains(guest.getName(), text) || contains(guest.getFirstName(), text)
                    || contains(guest.getCarPlate(), text) || contains(guest.getTrailerPlate(), text)
                    || cardHolders.contains(guest.getId())
                    || hasUnitWithPlate(guest, Unit.normalizePlate(search))) {
                found.add(guest);
            }
        }
        return found;
    }

    /**
     * Creates a guest in an ISSUER group of the user's scope.
     */
    @Transactional
    public Guest createGuest(Userinfo principal, Guest.Builder details) {
        Guest guest = details.build();
        if (guest.getGroup() == null || !GroupType.ISSUER.name().equals(guest.getGroup().getType())) {
            throw new IllegalArgumentException("A guest hangs in an ISSUER group: " + guest.getGroup());
        }
        if (guest.getName() == null || guest.getName().isBlank()) {
            throw new IllegalArgumentException("Name of the guest is missing");
        }
        checkIsManageable(principal, guest);
        guest.setCreateDate(dateService.getNow());
        guest = guestDao.save(guest);
        LOG.info(String.format("Guest %s created by %s", guest, principal.getUsername()));
        return guest;
    }

    /**
     * Copies the given details (null keeps the current value) onto the guest.
     */
    @Transactional
    public Guest updateGuest(Userinfo principal, Long guestId, Guest details) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        guest.updateDetails(details, dateService.getNow());
        return guestDao.save(guest);
    }

    @Transactional
    public Guest changeGuestStatus(Userinfo principal, Long guestId, GuestStatus status) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        guest.setStatus(status);
        guest.setModDate(dateService.getNow());
        LOG.info(String.format("Guest %s set to %s by %s", guest, status, principal.getUsername()));
        return guestDao.save(guest);
    }

    @Transactional
    public Unit addUnit(Userinfo principal, Long guestId, Unit.UnitType unitType, String plate, BigDecimal lengthM) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        if (unitType == null) {
            throw new IllegalArgumentException("Type of the unit is missing");
        }
        Unit unit = guest.addUnit(unitType, plate, lengthM);
        unit.setCreateDate(dateService.getNow());
        // the guest is managed: the unit is persisted (and gets its id) when the guest is flushed
        guestDao.flush();
        return unit;
    }

    /**
     * Opens a billing account for the guest; without a number the account waits for the ERP to assign one.
     */
    @Transactional
    public BillingAccount openBillingAccount(Userinfo principal, Long guestId, String erpAccountNo,
                                             String description) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        return openBillingAccount(guest, erpAccountNo, description);
    }

    BillingAccount openBillingAccount(Guest guest, String erpAccountNo, String description) {
        if (erpAccountNo != null) {
            if (!CampingCard.isValidBillingAccountNo(erpAccountNo)) {
                throw new IllegalArgumentException("Billing account number is not valid: " + erpAccountNo);
            }
            BillingAccount existing = billingAccountDao.findByErpAccountNo(erpAccountNo);
            if (existing != null) {
                throw new IllegalStateException(String.format("Billing account %s belongs to guest %s already",
                        erpAccountNo, existing.getGuest().getId()));
            }
        }
        BillingAccount account = guest.openBillingAccount(erpAccountNo, description);
        account.setCreateDate(dateService.getNow());
        return billingAccountDao.save(account);
    }

    /**
     * The guest arrives: a stay on the pitch from arrival to departure day. The card handed out for the stay is
     * activated and valid exactly for the stay, and billed to the given account of the guest (opened when the guest
     * has no account with the number yet).
     *
     * @param pitchNo          null for a free choice of pitch (tents' meadow)
     * @param cardId           null when the guest gets no card (no electricity)
     * @param unitId           null to take the guest's only unit with a plate
     * @param billingAccountNo null to keep the card's billing account
     */
    @Transactional
    public Stay checkIn(Userinfo principal, Long guestId, Long siteGroupId, String pitchNo, LocalDate arrival,
                        LocalDate departure, Long cardId, Long unitId, String billingAccountNo) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        if (guest.isDeactivated()) {
            throw new IllegalStateException("Guest " + guestId + " is deactivated");
        }
        Group siteGroup = siteGroupId != null ? groupService.fetchByGroupId(siteGroupId) : null;
        if (siteGroup == null || !GroupType.SITE.name().equals(siteGroup.getType())) {
            throw new IllegalArgumentException("Campsite of the stay must be a SITE group: " + siteGroupId);
        }
        checkIsManageableSite(principal, siteGroup);

        Stay stay = new Stay(guest, siteGroup, normalizePitch(pitchNo), arrival, departure);
        checkPitchIsFree(stay, arrival, departure);

        if (cardId != null) {
            CampingCard card = campingCardDao.findById(cardId).orElse(null);
            if (card == null || card.isDeleted()) {
                throw new NoSuchElementException("Card " + cardId + " not found");
            }
            if (card.getGuest() == null || !card.getGuest().equals(guest)) {
                throw new IllegalArgumentException(String.format("Card %s is not a card of guest %s", cardId,
                        guestId));
            }
            if (!stayDao.findOpenStaysOfCardOn(cardId, arrival).isEmpty()) {
                throw new IllegalStateException(String.format("Card %s is handed out for another stay on %s",
                        card.getCardNumber(), arrival));
            }
            if (billingAccountNo != null) {
                BillingAccount account = guest.findBillingAccount(billingAccountNo);
                if (account == null) {
                    account = openBillingAccount(guest, billingAccountNo, "Stay at " + siteGroup.getDisplayName()
                            + " from " + arrival);
                }
                if (account.isClosed()) {
                    throw new IllegalStateException("Billing account " + billingAccountNo + " is closed");
                }
                card.setBillingAccountNo(billingAccountNo);
            }
            // the stay is the validity of the card
            card.setValidFrom(arrival);
            card.setValidTo(departure);
            card.setActivation(CardActivation.ACTIVATED);
            stay.setCard(campingCardDao.save(card));
        }

        stay.setUnit(unitId != null ? getUnitOfGuest(guest, unitId) : guest.getOnlyUnitWithPlate());
        stay.setCheckInDate(dateService.getNow());
        stay.setCheckedInBy(principal.getUsername());
        stay = stayDao.save(stay);
        LOG.info(String.format("Guest %s checked in: %s at %s by %s", guest, stay, siteGroup.getName(),
                principal.getUsername()));
        return stay;
    }

    /**
     * The stay is extended or shortened; the card follows.
     */
    @Transactional
    public Stay changeDeparture(Userinfo principal, Long stayId, LocalDate departure) {
        Stay stay = getStay(stayId);
        checkIsManageable(principal, stay.getGuest());
        if (departure != null && departure.isAfter(stay.getDepartureDate())) {
            checkPitchIsFree(stay, stay.getDepartureDate(), departure);
        }
        stay.changeDeparture(departure);
        if (stay.getCard() != null) {
            campingCardDao.save(stay.getCard());
        }
        return stayDao.save(stay);
    }

    /**
     * The guest leaves. The card is valid until the end of the day at the latest; supply sessions still open on the
     * stay are closed by the stale-session job after the configured delay (the guest may still unplug).
     */
    @Transactional
    public Stay checkOut(Userinfo principal, Long stayId) {
        Stay stay = getStay(stayId);
        checkIsManageable(principal, stay.getGuest());
        if (stay.isCheckedOut()) {
            throw new IllegalStateException(String.format("Stay %s is checked out since %s", stayId,
                    stay.getCheckOutDate()));
        }
        stay.checkOut(dateService.getNow(), principal.getUsername());
        if (stay.getCard() != null) {
            campingCardDao.save(stay.getCard());
        }
        stay = stayDao.save(stay);
        LOG.info(String.format("Stay %s of guest %s checked out by %s", stay, stay.getGuest().getId(),
                principal.getUsername()));
        return stay;
    }

    @Transactional(readOnly = true)
    public List<Stay> findStaysOfGuest(Userinfo principal, Long guestId) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        return stayDao.findByGuestId(guestId);
    }

    /**
     * Stays at the campsites of the user's SITE scope (or at one of them) overlapping the days.
     *
     * @param state ALL, OPEN or CHECKED_OUT
     */
    @Transactional(readOnly = true)
    public List<Stay> findStays(Userinfo principal, Long siteGroupId, String state, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Period of the stays is missing or ends before it starts");
        }
        if (!List.of(STAYS_ALL, STAYS_OPEN, STAYS_CHECKED_OUT).contains(state)) {
            throw new IllegalArgumentException("Unknown stay state " + state);
        }
        Group siteScope = principal.getGroup(GroupType.SITE);
        if (siteScope == null) {
            LOG.info(String.format("User '%s' has no SITE scope, no stays", principal.getUsername()));
            return List.of();
        }
        List<Long> campsites = groupService.findManageable(siteScope);
        if (siteGroupId != null) {
            if (!campsites.contains(siteGroupId)) {
                throw new AccessDeniedException(String.format("User '%s' has no access to campsite %s",
                        principal.getUsername(), siteGroupId));
            }
            campsites = groupService.findManageable(groupService.fetchByGroupId(siteGroupId));
        }
        return stayDao.findStaysOfCampsites(campsites, state, from, to);
    }

    /**
     * Seasonal contracts on pitches of the user's SITE scope or of guests of its ISSUER scope.
     *
     * @param status null for all
     */
    @Transactional(readOnly = true)
    public List<SeasonalContract> findSeasonalContracts(Userinfo principal, ContractStatus status) {
        List<Long> campsites = manageableOrNone(principal.getGroup(GroupType.SITE));
        List<Long> issuers = manageableOrNone(principal.getGroup(GroupType.ISSUER));
        if (campsites.equals(NO_GROUPS) && issuers.equals(NO_GROUPS)) {
            return List.of();
        }
        return seasonalContractDao.findContractsInScope(campsites, issuers, status);
    }

    /**
     * Cards and contracts of a guest for its page.
     */
    @Transactional(readOnly = true)
    public List<CampingCard> findCardsOfGuest(Userinfo principal, Long guestId) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        return campingCardDao.findByGuestId(guestId);
    }

    @Transactional(readOnly = true)
    public List<SeasonalContract> findContractsOfGuest(Userinfo principal, Long guestId) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        return seasonalContractDao.findContractsOfGuest(guestId);
    }

    /**
     * The guest itself after the scope check, for its page.
     */
    @Transactional(readOnly = true)
    public Guest findManageableGuest(Userinfo principal, Long guestId) {
        Guest guest = getGuest(guestId);
        checkIsManageable(principal, guest);
        return guest;
    }

    private List<Long> manageableOrNone(Group scope) {
        if (scope == null) {
            return NO_GROUPS;
        }
        List<Long> ids = groupService.findManageable(scope);
        return ids.isEmpty() ? NO_GROUPS : ids;
    }

    private static boolean hasUnitWithPlate(Guest guest, String plate) {
        for (Unit unit : guest.getUnits()) {
            if (plate != null && !unit.isDeleted() && unit.getPlate() != null && unit.getPlate().contains(plate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findCheckedOutStayIds(Collection<Long> stayIds, LocalDateTime checkedOutBefore) {
        if (stayIds == null || stayIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(stayDao.findIdsCheckedOutBefore(stayIds, checkedOutBefore));
    }

    private void checkPitchIsFree(Stay stay, LocalDate from, LocalDate to) {
        if (stay.getPitchNo() == null) {
            return;
        }
        for (Stay other : stayDao.findOpenStaysOnPitch(stay.getSiteGroup().getId(), stay.getPitchNo(), from, to)) {
            if (!other.equals(stay)) {
                throw new IllegalStateException(String.format("Pitch %s is let to stay %s (%s - %s)",
                        stay.getPitchNo(), other.getId(), other.getArrivalDate(), other.getDepartureDate()));
            }
        }
    }

    private void checkIsManageable(Userinfo principal, Guest guest) {
        if (!userinfoService.isManageable(principal, guest)) {
            throw new AccessDeniedException(String.format("User '%s' has no access to guest %s",
                    principal.getUsername(), guest.getId()));
        }
    }

    private void checkIsManageableSite(Userinfo principal, Group siteGroup) {
        if (!groupService.isManageable(principal.getGroup(GroupType.SITE), siteGroup)) {
            throw new AccessDeniedException(String.format("User '%s' has no access to campsite %s",
                    principal.getUsername(), siteGroup.getName()));
        }
    }

    private static Unit getUnitOfGuest(Guest guest, Long unitId) {
        for (Unit unit : guest.getUnits()) {
            if (unitId.equals(unit.getId())) {
                return unit;
            }
        }
        throw new IllegalArgumentException(String.format("Unit %s is not a unit of guest %s", unitId,
                guest.getId()));
    }

    private Guest getGuest(Long guestId) {
        Guest guest = findGuest(guestId);
        if (guest == null || guest.isDeleted()) {
            throw new NoSuchElementException("Guest " + guestId + " not found");
        }
        return guest;
    }

    private Stay getStay(Long stayId) {
        Stay stay = stayId != null ? stayDao.findById(stayId).orElse(null) : null;
        if (stay == null || stay.isDeleted()) {
            throw new NoSuchElementException("Stay " + stayId + " not found");
        }
        return stay;
    }

    private static String normalizePitch(String pitchNo) {
        return pitchNo != null && !pitchNo.isBlank() ? pitchNo.trim().toUpperCase(Locale.ROOT) : null;
    }

    private static boolean contains(String value, String text) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(text);
    }
}
