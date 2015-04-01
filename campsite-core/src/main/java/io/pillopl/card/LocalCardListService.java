package io.pillopl.card;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.group.GroupService;
import io.pillopl.group.attribute.LocalListAttribute;
import io.pillopl.group.attribute.LocalListAttributeService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.LocalListVersionDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Local card lists in the pedestal controllers, so that guests can switch on supply while the pedestal is offline:
 * house cards first, then the valid camping cards of the issuers accepted at the campsite, cut to the capacity of
 * the controller (from the pedestal type), with a version per pedestal.
 */
@Service
public class LocalCardListService {

    private static final Logger LOG = LoggerFactory.getLogger(LocalCardListService.class);

    private final GroupService groupService;

    private final PedestalDao pedestalDao;

    private final LocalListAttributeService localListAttributeService;

    private final CampingCardDao campingCardDao;

    private final LocalListVersionDao localListVersionDao;

    private final HouseCardService houseCardService;

    private final LocalCardListSender localCardListSender;

    private final SystemPropertyService systemPropertyService;

    private final DateService dateService;

    public LocalCardListService(GroupService groupService, PedestalDao pedestalDao,
                                LocalListAttributeService localListAttributeService, CampingCardDao campingCardDao,
                                LocalListVersionDao localListVersionDao, HouseCardService houseCardService,
                                LocalCardListSender localCardListSender, SystemPropertyService systemPropertyService,
                                DateService dateService) {
        this.groupService = groupService;
        this.pedestalDao = pedestalDao;
        this.localListAttributeService = localListAttributeService;
        this.campingCardDao = campingCardDao;
        this.localListVersionDao = localListVersionDao;
        this.houseCardService = houseCardService;
        this.localCardListSender = localCardListSender;
        this.systemPropertyService = systemPropertyService;
        this.dateService = dateService;
    }

    /**
     * Sends the whole list to the pedestal.
     *
     * @return false when the pedestal keeps no local list (group attribute or controller)
     * @throws LocalCardListException when the list could not be sent
     */
    @Transactional
    public boolean sendFull(String pedestalCode, boolean useCache) {
        Pedestal pedestal = getPedestal(pedestalCode);

        // fetch the group attribute: is a local list allowed?
        LocalListAttribute lla = localListAttributeService.getLocalListAttribute(pedestal.getGroup());
        if (lla == null) {
            return false;
        }

        LocalCardList list = buildLocalCardList(pedestal, lla);
        if (list.capacity() == 0) {
            LOG.info(String.format("Controller of %s keeps no local card list", pedestal));
            return false;
        }

        // ... set constants
        boolean fullUpdate = true;
        Integer version = getNextLocalListVersion(pedestal);

        LocalCardListSendResult result = localCardListSender.sendLocalList(pedestal, fullUpdate, list.entries(),
                version, useCache);

        return requireSent(pedestalCode, result);
    }

    /**
     * Sends only the given house cards (added, changed or removed) to the pedestal.
     *
     * @throws LocalCardListException when the list could not be sent
     */
    @Transactional
    public boolean sendDifferential(String pedestalCode, List<HouseCard> houseCards, boolean useCache) {
        Pedestal pedestal = getPedestal(pedestalCode);

        boolean fullUpdate = false;
        Integer version = getNextLocalListVersion(pedestal);

        List<LocalCardListEntry> entries = new ArrayList<>();
        if (houseCards != null) {
            for (HouseCard houseCard : houseCards) {
                entries.add(LocalCardListEntry.of(houseCard));
            }
        }
        LocalCardListSendResult result = localCardListSender.sendLocalList(pedestal, fullUpdate, entries, version,
                useCache);

        return requireSent(pedestalCode, result);
    }

    /**
     * The list the pedestal gets now, without sending it.
     */
    @Transactional(readOnly = true)
    public LocalCardList buildLocalCardList(String pedestalCode) {
        Pedestal pedestal = getPedestal(pedestalCode);
        LocalListAttribute lla = localListAttributeService.getLocalListAttribute(pedestal.getGroup());
        if (lla == null) {
            return LocalCardList.empty(pedestalCode, getCapacity(pedestal));
        }
        return buildLocalCardList(pedestal, lla);
    }

    /**
     * House cards (used last first), then camping cards (newest first); house cards beyond the capacity push all
     * camping cards out.
     */
    LocalCardList buildLocalCardList(Pedestal pedestal, LocalListAttribute lla) {
        int capacity = getCapacity(pedestal);

        // ... fetch house cards if necessary
        List<HouseCard> houseCards = new ArrayList<>();
        if (lla.isIncludeHouseCards()) {
            houseCards = houseCardService.getHouseCardsForPedestal(pedestal.getCode());
        }

        // ... fetch cards if necessary
        List<CampingCard> cards = new ArrayList<>();
        if (lla.isIncludeCards()) {
            cards = campingCardDao.findValidCardsForLocalCardList(
                    groupService.findIssuerGroupIdsAcceptedAt(pedestal.getGroup()), dateService.getToday());
        }

        int available = houseCards.size() + cards.size();

        // ... check size of the list and cut if necessary
        if (cards.size() + houseCards.size() > capacity) {
            // house cards alone already above the limit?
            if (houseCards.size() > capacity) {
                // ... forget the cards
                cards = new ArrayList<>();
                // ... cut
                houseCards = sortAndCutHouseCards(houseCards, capacity);
            } else {
                cards = cutCards(cards, capacity - houseCards.size());
            }
        }

        List<LocalCardListEntry> entries = new ArrayList<>(houseCards.size() + cards.size());
        for (HouseCard houseCard : houseCards) {
            entries.add(LocalCardListEntry.of(houseCard));
        }
        for (CampingCard card : cards) {
            entries.add(LocalCardListEntry.of(card));
        }
        int dropped = available - entries.size();
        if (dropped > 0) {
            LOG.warn(String.format("Local card list of %s cut to %d entries, %d cards left out", pedestal, capacity,
                    dropped));
        }
        return new LocalCardList(pedestal.getCode(), capacity, entries, houseCards.size(), cards.size(), dropped);
    }

    @Transactional
    public Integer getNextLocalListVersion(Pedestal pedestal) {
        if (pedestal == null || pedestal.getId() == null) {
            throw new IllegalArgumentException("Persistent pedestal required");
        }

        LocalListVersion localListVersion = localListVersionDao.findByPedestal(pedestal);
        if (localListVersion == null) {
            // the first list the controller gets
            localListVersion = new LocalListVersion(pedestal, 1);
        } else {
            localListVersion.setVersion(localListVersion.getVersion() + 1);
        }
        localListVersion.setModDate(dateService.getNow());

        return localListVersionDao.save(localListVersion).getVersion();
    }

    /**
     * Sends a changed house card to every pedestal whose local list holds it; failures are logged.
     */
    @Transactional
    public void updateLocalListWith(Long houseCardId) {
        HouseCard houseCard = houseCardService.getHouseCard(houseCardId);
        if (houseCard == null) {
            LOG.info("House card with id " + houseCardId + " not found. Don't trigger local card list update");
            return;
        }

        for (Pedestal pedestal : houseCardService.findPedestalsForHouseCard(houseCard)) {
            try {
                sendDifferential(pedestal.getCode(), List.of(houseCard), true);
            } catch (Exception e) {
                LOG.error("Error updating the local card list of " + pedestal + " with " + houseCard, e);
            }
        }
    }

    int getCapacity(Pedestal pedestal) {
        if (pedestal.getPedestalType() == null) {
            return 0;
        }
        return pedestal.getPedestalType()
                .getEffectiveLocalCardListCapacity(systemPropertyService.getLocalCardListDefaultMaxEntries());
    }

    private Pedestal getPedestal(String pedestalCode) {
        Pedestal pedestal = pedestalCode != null ? pedestalDao.findByCode(pedestalCode) : null;
        if (pedestal == null || pedestal.isDeleted()) {
            throw new IllegalArgumentException(String.format("No pedestal found for code: %s", pedestalCode));
        }
        return pedestal;
    }

    private boolean requireSent(String pedestalCode, LocalCardListSendResult result) {
        if (!result.success()) {
            throw new LocalCardListException(pedestalCode, result.errorMessage());
        }
        return true;
    }

    private List<HouseCard> sortAndCutHouseCards(List<HouseCard> houseCards, final int maxListLength) {
        List<HouseCard> sorted = new ArrayList<>(houseCards);
        sorted.sort(Comparator.comparing(HouseCard::getLastUsed, Comparator.nullsLast(Comparator.reverseOrder())));
        return new ArrayList<>(sorted.subList(0, Math.min(maxListLength, sorted.size())));
    }

    private List<CampingCard> cutCards(List<CampingCard> cards, int maxListLength) {
        return new ArrayList<>(cards.subList(0, Math.max(0, Math.min(maxListLength, cards.size()))));
    }
}
