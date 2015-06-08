package io.pillopl.group.attribute;

import io.pillopl.card.CampingCard;
import io.pillopl.card.HouseCard;
import io.pillopl.card.HouseCardService;
import io.pillopl.card.HouseCardType;
import io.pillopl.group.AccessMode;
import io.pillopl.group.AffiliateGroupService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupServiceConstants;
import io.pillopl.group.Market;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.WelcomeCardAdoptionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Whether a welcome card may be adopted at pedestals of a SITE group, and the adoption itself: a welcome card of the
 * market's pool, refused at a pedestal of an affiliate campsite with house cards only, becomes a house card of that
 * campsite when the campsite allows the adoption.
 */
@Service
public class WelcomeCardAdoptionService {

    private static final Logger LOG = LoggerFactory.getLogger(WelcomeCardAdoptionService.class);

    /**
     * Name prefix of the affiliate campsite group, followed by the owner's username.
     */
    private static final String AFFILIATE_NAME_PREFIX = GroupServiceConstants.PREFIX_AFFILIATE + " ";

    private final WelcomeCardAdoptionDao welcomeCardAdoptionDao;

    private final PedestalDao pedestalDao;

    private final CampingCardDao campingCardDao;

    private final GroupService groupService;

    private final AffiliateGroupService affiliateGroupService;

    private final HouseCardService houseCardService;

    public WelcomeCardAdoptionService(WelcomeCardAdoptionDao welcomeCardAdoptionDao, PedestalDao pedestalDao,
                                      CampingCardDao campingCardDao, GroupService groupService,
                                      AffiliateGroupService affiliateGroupService, HouseCardService houseCardService) {
        this.welcomeCardAdoptionDao = welcomeCardAdoptionDao;
        this.pedestalDao = pedestalDao;
        this.campingCardDao = campingCardDao;
        this.groupService = groupService;
        this.affiliateGroupService = affiliateGroupService;
        this.houseCardService = houseCardService;
    }

    /**
     * Bubbles up the hierarchy: the nearest group with an attribute decides, no attribute at all means no.
     */
    @Transactional(readOnly = true)
    public boolean adoptionIsAllowedFor(Group group) {
        if (group != null) {
            WelcomeCardAdoption adoption = welcomeCardAdoptionDao.findForGroup(group.getId());
            if (adoption != null) {
                return adoption.isAllowed();
            } else {
                return adoptionIsAllowedFor(group.getParent());
            }
        } else {
            return false;
        }
    }

    /**
     * Adopts the card as house card of the affiliate campsite of the pedestal when it is a welcome card of the
     * market's pool, the campsite allows the adoption and accepts house cards only. The welcome card is deleted.
     *
     * @return true when the card was adopted
     */
    @Transactional
    public boolean checkAndAdopt(String pedestalCode, String cardRfid) {
        Pedestal pedestal = pedestalCode != null ? pedestalDao.findByCode(pedestalCode) : null;
        if (pedestal == null || pedestal.getGroup() == null) {
            LOG.info("Card {} not adopted: pedestal {} unknown or without group", cardRfid, pedestalCode);
            return false;
        }

        Market market = groupService.determineMarketByGroupId(pedestal.getGroup().getId());
        CampingCard card = findWelcomeCard(cardRfid, market);
        if (checkCard(card) && checkPedestal(pedestal)) {
            // find the affiliate campsite owner
            String owner = getAffiliateOwner(pedestal);
            // create the house card of the affiliate campsite
            HouseCard houseCard = houseCardService.saveHouseCard(owner, owner, null, HouseCardType.STAFF,
                    card.getCardRfid(), card.getCardNumber(), true);
            // the welcome card leaves the pool
            deleteCard(card);
            LOG.info(String.format("Welcome card %s adopted as house card %s of %s", cardRfid, houseCard.getId(),
                    owner));
            return true;
        } else {
            LOG.info("Card {} not adopted at {}: {}", cardRfid, pedestal,
                    card == null ? "not a welcome pool card" : "the campsite does not adopt welcome cards");
            return false;
        }
    }

    /**
     * The card with the UID when it belongs to a guest of the market's welcome card pool, otherwise null.
     */
    CampingCard findWelcomeCard(String cardRfid, Market market) {
        if (market == null || cardRfid == null) {
            return null;
        }
        Group pool = groupService.getExtensionByMarket(market).findWelcomeCardPoolIssuerGroup();
        if (pool == null) {
            return null;
        }
        CampingCard card = campingCardDao.findByCardRfid(cardRfid);
        if (card != null && card.getGuest() != null && card.getGuest().getGroup() != null
                && pool.getId().equals(card.getGuest().getGroup().getId())) {
            return card;
        }
        return null;
    }

    /**
     * The campsite must allow the adoption (nearest attribute) and accept house cards only.
     */
    boolean checkPedestal(Pedestal pedestal) {
        Group affiliateGroup = getAffiliateGroup(pedestal.getGroup());
        return adoptionIsAllowedFor(pedestal.getGroup())
                && affiliateGroup != null
                && AccessMode.HOUSE_CARDS_ONLY == affiliateGroupService.checkAccessMode(affiliateGroup.getId());
    }

    private boolean checkCard(CampingCard card) {
        return card != null;
    }

    private void deleteCard(CampingCard card) {
        card.setDeleted(true);
        campingCardDao.save(card);
    }

    static Group getAffiliateGroup(Group group) {
        if (group == null) {
            return null;
        }
        if (group.getName().startsWith(AFFILIATE_NAME_PREFIX)) {
            return group;
        }
        return getAffiliateGroup(group.getParent());
    }

    static String getAffiliateOwner(Pedestal pedestal) {
        Group group = getAffiliateGroup(pedestal.getGroup());
        return group != null ? group.getName().substring(AFFILIATE_NAME_PREFIX.length()) : null;
    }
}
