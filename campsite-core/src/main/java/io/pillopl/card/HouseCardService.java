package io.pillopl.card;

import io.pillopl.event.EventService;
import io.pillopl.event.HouseCardChangedEvent;
import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupServiceHelper;
import io.pillopl.group.GroupType;
import io.pillopl.group.attribute.LocalListAttribute;
import io.pillopl.group.attribute.LocalListAttributeService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.HouseCardDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.user.Userinfo;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * House cards of the campsites: which house cards a pedestal accepts (the cards of its whole parent line), the
 * console administration by the campsite owner or by staff managing the owner, and the triggers for the local card
 * lists.
 */
@Service
public class HouseCardService {

    private static final Logger LOG = LoggerFactory.getLogger(HouseCardService.class);

    private final UserinfoDao userinfoDao;

    private final GroupService groupService;

    private final HouseCardDao houseCardDao;

    private final PedestalDao pedestalDao;

    private final DateService dateService;

    private final EventService eventService;

    private final LocalListAttributeService localListAttributeService;

    public HouseCardService(UserinfoDao userinfoDao, GroupService groupService, HouseCardDao houseCardDao,
                            PedestalDao pedestalDao, DateService dateService, EventService eventService,
                            LocalListAttributeService localListAttributeService) {
        this.userinfoDao = userinfoDao;
        this.groupService = groupService;
        this.houseCardDao = houseCardDao;
        this.pedestalDao = pedestalDao;
        this.dateService = dateService;
        this.eventService = eventService;
        this.localListAttributeService = localListAttributeService;
    }

    /**
     * The active house card with the UID accepted at the pedestal, or null.
     */
    @Transactional(readOnly = true)
    public HouseCard getHouseCardForPedestal(String pedestalCode, String rfid) {
        List<HouseCard> houseCards = getHouseCardsForPedestal(pedestalCode);
        for (HouseCard houseCard : houseCards) {
            if (houseCard != null && houseCard.getRfid() != null && houseCard.getRfid().equals(rfid)) {
                return houseCard;
            }
        }
        return null;
    }

    /**
     * Active house cards of the pedestal's group and all its ancestors; empty for an unknown pedestal.
     */
    @Transactional(readOnly = true)
    public List<HouseCard> getHouseCardsForPedestal(String pedestalCode) {
        Pedestal pedestal = pedestalDao.findByCode(pedestalCode);
        if (pedestal == null || pedestal.getGroup() == null) {
            return new ArrayList<>();
        }
        List<Long> parentLineIds = groupService.fetchParentLineIds(pedestal.getGroup().getId(), true);

        List<HouseCard> houseCards = houseCardDao.findActiveByGroupIds(parentLineIds);

        List<HouseCard> res = new ArrayList<>();
        for (HouseCard houseCard : houseCards) {
            if (houseCard != null) {
                res.add(houseCard);
            }
        }
        return res;
    }

    /**
     * House cards the campsite owner sees in the console: those of the owner's SITE subtree (also deactivated ones).
     */
    @Transactional(readOnly = true)
    public List<HouseCard> getHouseCards(String username, String ownerUsername) {
        Userinfo owner = checkUserPrivileges(username, ownerUsername);
        Group group = getOwnerSiteGroup(owner);

        List<Long> groupIds = GroupServiceHelper.extractAllManageableGroupIDsForGroup(group, null);

        return getHouseCardsForGroupIdList(groupIds);
    }

    @Transactional(readOnly = true)
    public HouseCard getHouseCard(Long houseCardId) {
        return houseCardId != null ? houseCardDao.findById(houseCardId).orElse(null) : null;
    }

    /**
     * Creates or changes a house card of the owner's campsite.
     *
     * @param houseCardId null for a new card
     */
    @Transactional
    public HouseCard saveHouseCard(String username, String ownerUsername, Long houseCardId,
                                   HouseCardType houseCardType, String rfid, String cardNumber, boolean active) {
        Userinfo owner = checkUserPrivileges(username, ownerUsername);
        if (houseCardType == null) {
            throw new IllegalArgumentException("House card type is missing");
        }
        if (rfid == null || rfid.isBlank()) {
            throw new IllegalArgumentException("Card UID of the house card is missing");
        }

        HouseCard houseCard = null;
        if (houseCardId != null) {
            houseCard = houseCardDao.findById(houseCardId).orElse(null);
            checkOwnedBy(houseCard, owner);
        }
        if (houseCard == null) {
            houseCard = new HouseCard();
            houseCard.setCreationDate(dateService.getNow());
        }

        houseCard.setActive(active);
        houseCard.setRfid(rfid.trim());
        houseCard.setCardNumber(cardNumber);
        houseCard.setHouseCardType(houseCardType);
        houseCard.setGroup(getOwnerSiteGroup(owner));

        houseCard = houseCardDao.save(houseCard);
        triggerLocalListUpdate(houseCard.getId());
        return houseCard;
    }

    @Transactional
    public boolean deleteHouseCard(String username, String ownerUsername, Long houseCardId) {
        Userinfo owner = checkUserPrivileges(username, ownerUsername);

        HouseCard houseCard = houseCardId != null ? houseCardDao.findById(houseCardId).orElse(null) : null;
        if (houseCard == null || houseCard.isDeleted()) {
            return false;
        }
        checkOwnedBy(houseCard, owner);
        houseCard.markDeleted(dateService.getNow());
        houseCardDao.save(houseCard);
        triggerLocalListUpdate(houseCard.getId());
        return true;
    }

    @Transactional
    public boolean activateHouseCard(String username, String ownerUsername, Long houseCardId) {
        return changeActive(username, ownerUsername, houseCardId, true);
    }

    @Transactional
    public boolean deactivateHouseCard(String username, String ownerUsername, Long houseCardId) {
        return changeActive(username, ownerUsername, houseCardId, false);
    }

    /**
     * Remembers the last use (the cards used last stay on a full local card list).
     */
    @Transactional
    public void recordUse(HouseCard houseCard) {
        if (houseCard == null || houseCard.getId() == null) {
            return;
        }
        houseCardDao.findById(houseCard.getId()).ifPresent(hc -> hc.setLastUsed(dateService.getNow()));
    }

    /**
     * The local card lists of the campsite's pedestals need the change; failures are logged.
     */
    public void triggerLocalListUpdate(Long houseCardId) {
        try {
            HouseCardChangedEvent event = new HouseCardChangedEvent(houseCardId);
            LOG.debug(String.format("Created HOUSE_CARD_CHANGED event %s", event));
            eventService.send(event);
        } catch (Exception e) {
            LOG.error("Error sending local card list update.", e);
        }
    }

    /**
     * Pedestals whose local card list holds the house card: pedestals with a local list in the card's SITE subtree,
     * when the nearest local list attribute takes house cards in.
     */
    @Transactional(readOnly = true)
    public List<Pedestal> findPedestalsForHouseCard(HouseCard houseCard) {
        List<Pedestal> emptyList = new ArrayList<>(0);
        if (houseCard == null) {
            return emptyList;
        }

        try {
            LocalListAttribute lla = localListAttributeService.getLocalListAttribute(houseCard.getGroup());
            if (lla == null || !lla.isIncludeHouseCards()) {
                LOG.info("No local card list update configured for house card with id " + houseCard.getId()
                        + ". Don't trigger local card list update");
                return emptyList;
            }

            List<Pedestal> result = new ArrayList<>();
            for (Pedestal pedestal : pedestalDao.findByGroupIds(groupService.findManageable(houseCard.getGroup()))) {
                if (localListAttributeService.isLocalListPedestal(pedestal)) {
                    result.add(pedestal);
                }
            }
            if (result.isEmpty()) {
                LOG.info("No pedestals available for local card list update");
            }
            return result;
        } catch (Exception e) {
            LOG.error("Error finding pedestals for the local card list update of " + houseCard, e);
            return emptyList;
        }
    }

    /**
     * The calling user may administer the house cards of the owner: it is the owner, or the owner's own STAFF group
     * lies in the subtree of the caller's STAFF scope (in-memory walk over the loaded tree).
     *
     * @return the owner
     */
    @Transactional(readOnly = true)
    public Userinfo checkUserPrivileges(String username, String ownerUsername) {
        Userinfo user = userinfoDao.findByUsername(username);

        // the owner administers its own cards
        if (user != null && username.equals(ownerUsername)) return user;

        if (user == null) throw new NoSuchElementException("No user " + username + " found.");

        Userinfo owner = userinfoDao.findByUsername(ownerUsername);
        if (owner == null) throw new NoSuchElementException("No campsite owner " + ownerUsername + " found.");

        Group group = user.getGroup(GroupType.STAFF);
        if (group == null) {
            throw new AccessDeniedException("User " + username + " is not allowed to administer users");
        }
        List<Long> allGroupIds = new ArrayList<>(GroupServiceHelper.extractAllManageableGroupIDsForGroup(group, null));

        Group ownerGroup = owner.getGroup();
        if (ownerGroup == null) {
            throw new AccessDeniedException("User " + username + " is not allowed to administer user " + ownerUsername);
        }

        if (!allGroupIds.contains(ownerGroup.getId())) {
            throw new AccessDeniedException("User " + username + " is not allowed to administer user " + ownerUsername);
        }

        return owner;
    }

    private boolean changeActive(String username, String ownerUsername, Long houseCardId, boolean active) {
        Userinfo owner = checkUserPrivileges(username, ownerUsername);

        HouseCard houseCard = houseCardId != null ? houseCardDao.findById(houseCardId).orElse(null) : null;
        if (houseCard == null || houseCard.isDeleted()) {
            return false;
        }
        checkOwnedBy(houseCard, owner);
        houseCard.setActive(active);
        houseCardDao.save(houseCard);
        triggerLocalListUpdate(houseCard.getId());
        return true;
    }

    private List<HouseCard> getHouseCardsForGroupIdList(List<Long> groupIds) {
        List<HouseCard> houseCards = new ArrayList<>();
        if (groupIds != null && !groupIds.isEmpty()) {
            Set<HouseCard> unique = new LinkedHashSet<>(houseCardDao.findForGroupList(groupIds));
            houseCards.addAll(unique);
        }
        return houseCards;
    }

    private Group getOwnerSiteGroup(Userinfo owner) {
        Group group = owner.getGroup(GroupType.SITE);
        if (group == null) {
            throw new IllegalStateException("User " + owner.getUsername() + " has no campsite (SITE group)");
        }
        return group;
    }

    private void checkOwnedBy(HouseCard houseCard, Userinfo owner) {
        if (houseCard == null) {
            return;
        }
        Group ownerGroup = getOwnerSiteGroup(owner);
        if (!GroupServiceHelper.extractAllManageableGroupIDsForGroup(ownerGroup, null)
                .contains(houseCard.getGroupId())) {
            throw new AccessDeniedException(String.format("House card %s does not belong to the campsite of %s",
                    houseCard.getId(), owner.getUsername()));
        }
    }
}
