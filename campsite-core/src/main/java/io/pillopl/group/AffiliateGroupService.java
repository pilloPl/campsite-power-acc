package io.pillopl.group;

import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.GroupDao;
import io.pillopl.persistence.dao.PedestalDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Affiliate campsites: independent campsites in the network, one SITE group "AFFILIATE &lt;username&gt;" per owner,
 * hanging under "NETWORK ACCESS &lt;MKT&gt;" or "HOUSE CARDS ONLY &lt;MKT&gt;".
 */
@Service
public class AffiliateGroupService {

    private static final Logger LOG = LoggerFactory.getLogger(AffiliateGroupService.class);

    private final GroupDao groupDao;

    private final GroupService groupService;

    private final GroupManager groupManager;

    private final PedestalDao pedestalDao;

    public AffiliateGroupService(GroupDao groupDao, GroupService groupService, GroupManager groupManager,
                                 PedestalDao pedestalDao) {
        this.groupDao = groupDao;
        this.groupService = groupService;
        this.groupManager = groupManager;
        this.pedestalDao = pedestalDao;
    }

    @Transactional(readOnly = true)
    public Group findAffiliateSiteGroup(String username) {
        return groupDao.findByNameAndType(GroupServiceHelper.getAffiliateGroupName(username), GroupType.SITE.name());
    }

    /**
     * Creates the group of a new affiliate campsite under the parent of the requested access mode.
     */
    @Transactional
    public Group createAffiliateSiteGroup(String username, String displayName, Market market, AccessMode accessMode) {
        Group parent = groupService.getExtensionByMarket(market).findAccessModeSiteGroup(accessMode);
        if (parent == null) {
            throw new IllegalStateException(String.format("No %s group for market %s", accessMode, market));
        }
        return groupManager.createChildGroup(parent, GroupServiceHelper.getAffiliateGroupName(username), displayName,
                GroupType.SITE);
    }

    /**
     * Moves the affiliate campsite group under "NETWORK ACCESS &lt;MKT&gt;" or "HOUSE CARDS ONLY &lt;MKT&gt;" and sets the
     * flags of its pedestals: with network access they are listed in the app and remote controlled as requested;
     * with house cards only they are neither listed nor remote controlled nor settled by the campsite.
     *
     * @return the access mode after the switch, or null (logged) when the group is not an affiliate group
     */
    @Transactional
    public AccessMode switchAccessMode(Long groupId, AccessMode targetMode, boolean listedInApp) {
        if (targetMode == AccessMode.HOUSE_CARDS_ONLY && listedInApp) {
            LOG.warn("House cards only with listed in app seems not reasonable");
        }

        Market market = groupService.determineMarketByGroupId(groupId);
        MarketExtension<Market> extension = groupService.getExtensionByMarket(market != null ? market : Market.UNKNOWN);
        Group houseCardsOnlyGroup = extension.findHouseCardsOnlySiteGroup();
        Group networkAccessGroup = extension.findNetworkAccessSiteGroup();
        Group affiliateGroup = groupId != null ? groupDao.findById(groupId).orElse(null) : null;
        AccessMode accessMode = checkAccessMode(groupId);
        if (affiliateGroup != null && accessMode != null) {
            if (networkAccessGroup != null && houseCardsOnlyGroup != null) {
                List<Pedestal> pedestals = pedestalDao.findByGroupIds(List.of(affiliateGroup.getId()));
                if (targetMode == AccessMode.NETWORK_ACCESS) { // switch to network access
                    affiliateGroup.setParent(networkAccessGroup);
                    for (Pedestal pedestal : pedestals) {
                        pedestal.setListedInApp(listedInApp);
                        // when the pedestal is not listed in the app it should not be remote controlled
                        pedestal.setRemoteControl(listedInApp);
                        pedestalDao.save(pedestal);
                    }
                } else { // switch to house cards only
                    affiliateGroup.setParent(houseCardsOnlyGroup);
                    for (Pedestal pedestal : pedestals) {
                        pedestal.setListedInApp(false);
                        pedestal.setRemoteControl(false);
                        pedestal.setSettledByCampsite(false);
                        pedestalDao.save(pedestal);
                    }
                }
                groupDao.saveAndFlush(affiliateGroup);
            } else {
                LOG.error(String.format("Under the SITE root there should be one %s and one %s group "
                        + "(affiliate group id: %s)", GroupServiceConstants.NETWORK_ACCESS,
                        GroupServiceConstants.HOUSE_CARDS_ONLY, groupId));
            }
            accessMode = checkAccessMode(affiliateGroup);
        } else {
            LOG.error(String.format("Seems that the group is not set up right, affiliate group id: %s", groupId));
        }

        return accessMode;
    }

    /**
     * Access mode of an affiliate campsite group. Only takes the parent into account!
     *
     * @return the access mode or null (logged) when the group is not an affiliate group or its parent name has
     * no access mode prefix
     */
    @Transactional(readOnly = true)
    public AccessMode checkAccessMode(Long groupId) {
        Group group = groupId != null ? groupDao.findById(groupId).orElse(null) : null;
        AccessMode accessMode = checkAccessMode(group);
        if (accessMode == null) {
            LOG.warn(String.format("Could not determine access mode for group with id: %s", groupId));
        }
        return accessMode;
    }

    static AccessMode checkAccessMode(Group group) {
        if (group != null && isAffiliate(group) && group.getParent() != null) {
            if (group.getParent().getName().startsWith(GroupServiceConstants.NETWORK_ACCESS)) {
                return AccessMode.NETWORK_ACCESS;
            }
            if (group.getParent().getName().startsWith(GroupServiceConstants.HOUSE_CARDS_ONLY)) {
                return AccessMode.HOUSE_CARDS_ONLY;
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public boolean isAffiliate(Long groupId) {
        Group g = groupId != null ? groupDao.findById(groupId).orElse(null) : null;
        return g != null && isAffiliate(g);
    }

    /**
     * A SITE group named "AFFILIATE &lt;username&gt;" (the bare prefix without a username does not count).
     */
    static boolean isAffiliate(Group group) {
        Objects.requireNonNull(group);
        String prefix = GroupServiceConstants.PREFIX_AFFILIATE + " ";
        return GroupType.SITE.isGroupType(group)
                && group.getName().startsWith(prefix)
                && !group.getName().endsWith(prefix);
    }
}
