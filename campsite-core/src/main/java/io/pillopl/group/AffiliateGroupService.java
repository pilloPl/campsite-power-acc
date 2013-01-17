package io.pillopl.group;

import io.pillopl.persistence.dao.GroupDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AffiliateGroupService(GroupDao groupDao, GroupService groupService, GroupManager groupManager) {
        this.groupDao = groupDao;
        this.groupService = groupService;
        this.groupManager = groupManager;
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
