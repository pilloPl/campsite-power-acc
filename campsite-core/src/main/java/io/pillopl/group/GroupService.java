package io.pillopl.group;

import io.pillopl.card.CampingCard;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.GroupDao;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.user.Userinfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Organisation trees: lookups by the names that carry logic, administrative scope by subtree, markets from name
 * prefixes and acceptance agreements (one semantics, see {@link GroupServiceHelper#getAcceptanceAgreements}).
 */
@Service
public class GroupService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupService.class);

    private final GroupDao groupDao;

    private final GroupManager groupManager;

    private final UserinfoDao userinfoDao;

    private final PedestalDao pedestalDao;

    private final GuestDao guestDao;

    public GroupService(GroupDao groupDao, GroupManager groupManager, UserinfoDao userinfoDao,
                        PedestalDao pedestalDao, GuestDao guestDao) {
        this.groupDao = groupDao;
        this.groupManager = groupManager;
        this.userinfoDao = userinfoDao;
        this.pedestalDao = pedestalDao;
        this.guestDao = guestDao;
    }

    /**
     * Consistency check of the trees, run by an integration test and by the nightly control job.
     * <p>
     * Checks the type hierarchy, the agreements, that console users hang in STAFF groups, guests in ISSUER groups and
     * pedestals in SITE groups.
     *
     * @return true when no error was found (errors are logged)
     */
    @Transactional(readOnly = true)
    public boolean checkAllGroupsForValidity() {
        return collectValidityErrors().isEmpty();
    }

    /**
     * The checks of {@link #checkAllGroupsForValidity()}, with the errors found (also logged) for the control mail.
     */
    @Transactional(readOnly = true)
    public List<String> collectValidityErrors() {
        Set<Long> checked = new HashSet<>();
        List<String> errors = new ArrayList<>();
        List<Group> all = groupDao.findAll();

        // check for valid type-hierarchy
        for (Group g : all) {
            GroupServiceHelper.checkParent(g, errors, checked);
        }

        // agreements only ISSUER -> SITE
        for (Group g : all) {
            GroupServiceHelper.checkAgreements(g, errors);
        }

        // check for type and correctly attached objects, for users, guests and pedestals,
        // to make sure they are correctly attached to their trees

        // find all users
        List<Userinfo> users = userinfoDao.findAll();

        for (Userinfo user : users) {
            if (user.getGroup() == null) {
                // a user without group is allowed (not yet set up)
                continue;
            }

            if (!GroupType.STAFF.isGroupType(user.getGroup())) {
                errors.add(String.format("User %s is attached to the wrong group %s", user,
                        user.getGroup().getType()));
            }
        }

        // find all guests
        List<Guest> guests = guestDao.findAll();

        for (Guest guest : guests) {
            if (guest.getGroup() == null) {
                errors.add(String.format("No group attached to Guest %s", guest));
                continue;
            }
            if (!GroupType.ISSUER.isGroupType(guest.getGroup())) {
                errors.add(String.format("Guest %s is attached to the wrong group %s", guest,
                        guest.getGroup().getType()));
            }
        }

        // find all pedestals
        List<Pedestal> pedestals = pedestalDao.findAll();

        for (Pedestal pedestal : pedestals) {
            if (pedestal.getGroup() == null) {
                errors.add(String.format("No group attached to Pedestal %s", pedestal));
                continue;
            }
            if (!GroupType.SITE.isGroupType(pedestal.getGroup())) {
                errors.add(String.format("Pedestal %s is attached to the wrong group %s", pedestal,
                        pedestal.getGroup().getType()));
            }
        }

        for (String error : errors) {
            LOG.error(error);
        }
        return errors;
    }

    @Transactional(readOnly = true)
    public boolean checkIsManageable(Group group, Group groupIsManageable) {
        if (isManageable(group, groupIsManageable)) {
            return true;
        }
        throw new AccessDeniedException(String.format("Group '%s' has no access to group '%s'",
                GroupServiceHelper.groupToPrettyString(group),
                GroupServiceHelper.groupToPrettyString(groupIsManageable)));
    }

    /**
     * Administrative scope: a group manages its whole subtree (including itself).
     */
    @Transactional(readOnly = true)
    public boolean isManageable(Group group, Group groupIsManageable) {
        if (group == null) {
            LOG.error("Admins group is null");
            return false;
        }

        // traverse referenced groups
        List<Long> ids = groupDao.findManageable(group.getId());
        if (groupIsManageable != null && ids.contains(groupIsManageable.getId())) {
            return true;
        }

        LOG.info(String.format("Group '%s' has no access to group '%s'", GroupServiceHelper.groupToPrettyString(group),
                GroupServiceHelper.groupToPrettyString(groupIsManageable)));
        return false;
    }

    /**
     * Walks up the parent line until a name starts with a market prefix ("HR ", "IT ", "FR ").
     *
     * @return the market or null when the group does not exist or no group on the line has a market prefix
     */
    @Transactional(readOnly = true)
    public Market determineMarketByGroupId(Long groupId) {
        Market market = null;

        Group group = groupId != null ? groupDao.findById(groupId).orElse(null) : null;

        if (group == null) {
            return null;
        }
        while ((market = GroupServiceHelper.checkMarket(group)) == null) {
            group = group.getParent();
            if (group == null) {
                break;
            }
        }
        return market;
    }

    @Transactional(readOnly = true)
    public String findMarketFromParentGroups(Long groupId) {
        Market market = determineMarketByGroupId(groupId);
        return market != null ? market.name() : null;
    }

    @Transactional(readOnly = true)
    public Group fetchByGroupId(Long groupId) {
        return groupDao.findById(groupId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Group fetchIssuerGroup(String groupName) {
        return groupDao.findByNotCaseSensitiveNameAndType(groupName, GroupType.ISSUER.name());
    }

    @Transactional(readOnly = true)
    public Group fetchSiteGroup(String groupName) {
        return groupDao.findByNameAndType(groupName, GroupType.SITE.name());
    }

    @Transactional(readOnly = true)
    public Group fetchStaffGroup(String groupName) {
        return groupDao.findByNotCaseSensitiveNameAndType(groupName, GroupType.STAFF.name());
    }

    @Transactional(readOnly = true)
    public Group fetchNetworkCardIssuerGroup() {
        return groupDao.findByNameAndType(GroupServiceConstants.NETWORK_CARD, GroupType.ISSUER.name());
    }

    @Transactional(readOnly = true)
    public List<Group> fetchAllGroupsByType(GroupType type) {
        List<Group> result = new ArrayList<>();
        for (Group root : groupDao.findRoots(type.name())) {
            result.addAll(GroupServiceHelper.extractAllManageableGroupsForGroup(root));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Long> fetchSiteGroupIds(Market market, String searchString) {
        Group root = getExtensionByMarket(market).findSiteRoot();
        return GroupServiceHelper.extractAllManageableGroupIDsForGroup(root, searchString);
    }

    /**
     * This method creates a list of the groups starting at the child node going up to the root.
     *
     * @param id                the child node to start
     * @param order_ChildToRoot true = ordered from child to root, false = ordered from root to child
     * @return a list of groups representing the line to or from the root
     */
    @Transactional(readOnly = true)
    public List<Group> fetchParentLine(Long id, boolean order_ChildToRoot) {
        List<Group> parentLineGroupList = new ArrayList<>();

        if (id != null) {
            Group childGroup = groupDao.findById(id).orElse(null);
            if (childGroup != null) {
                parentLineGroupList.add(childGroup);

                while (childGroup.getParent() != null) {
                    childGroup = childGroup.getParent();
                    parentLineGroupList.add(childGroup);
                }

                if (!order_ChildToRoot) Collections.reverse(parentLineGroupList);
            }
        }
        return parentLineGroupList;
    }

    @Transactional(readOnly = true)
    public List<Long> fetchParentLineIds(Long id, boolean order_ChildToRoot) {
        List<Long> parentLineIdList = new ArrayList<>();
        for (Group group : fetchParentLine(id, order_ChildToRoot)) {
            parentLineIdList.add(group.getId());
        }
        return parentLineIdList;
    }

    @Transactional(readOnly = true)
    public Set<Long> findAllManageableGroupIDsForGroupIDs(Long[] groupIds) {
        Set<Long> result = new LinkedHashSet<>();
        if (groupIds != null) {
            for (Long groupId : groupIds) {
                try {
                    Group group = groupDao.findById(groupId).orElse(null);
                    result.addAll(findManageable(group));
                } catch (Exception e) {
                    LOG.warn(String.format("Error reading manageable group for id %d", groupId), e);
                }
            }
        }
        return result;
    }

    /**
     * In-memory walk: ids of the SITE groups where cards of the issuer group are accepted.
     */
    @Transactional(readOnly = true)
    public List<Long> extractAllAcceptingSiteGroupIds(Group issuerGroup) {
        return GroupServiceHelper.extractAllAcceptingSiteGroupIds(issuerGroup);
    }

    /**
     * The same answer as {@link #extractAllAcceptingSiteGroupIds(Group)}, computed by the recursive SQL.
     */
    @Transactional(readOnly = true)
    public List<Long> findAcceptingSiteGroupIds(Group issuerGroup) {
        Objects.requireNonNull(issuerGroup);
        return groupDao.findAcceptingGroupIds(issuerGroup.getId(), GroupType.SITE.name());
    }

    /**
     * Whose cards are accepted in this SITE group (e.g. for the card lists of its pedestals), same semantics.
     */
    @Transactional(readOnly = true)
    public List<Long> findIssuerGroupIdsAcceptedAt(Group siteGroup) {
        Objects.requireNonNull(siteGroup);
        if (!GroupType.SITE.isGroupType(siteGroup)) {
            throw new IllegalStateException("Not a SITE group: " + GroupServiceHelper.groupToPrettyString(siteGroup));
        }
        return groupDao.findGroupIdsAcceptedAt(siteGroup.getId(), GroupType.ISSUER.name(), GroupType.SITE.name());
    }

    /**
     * True when cards of the issuer group are accepted in the given SITE group.
     */
    @Transactional(readOnly = true)
    public boolean isAcceptedAt(Group issuerGroup, Long siteGroupId) {
        if (issuerGroup == null || siteGroupId == null) {
            return false;
        }
        List<Long> allGroupIds = GroupServiceHelper.extractAllAcceptingSiteGroupIds(issuerGroup);
        return allGroupIds.contains(siteGroupId);
    }

    /**
     * Is the card accepted at the pedestal? Card, guest, the guest's ISSUER group, its accepting SITE subtrees
     * (in-memory walk, the one agreement rule), then the DAO checks the pedestal against those groups.
     */
    @Transactional(readOnly = true)
    public boolean isCardAcceptedAt(final Long pedestalId, final CampingCard card) {
        if (card == null || card.getGuest() == null) {
            return false;
        }
        Group issuerGroup = card.getGuest().getGroup();
        List<Long> allGroupIds = GroupServiceHelper.extractAllAcceptingSiteGroupIds(issuerGroup);
        Pedestal pedestal = (allGroupIds != null && !allGroupIds.isEmpty())
                ? pedestalDao.isInGroupList(pedestalId, allGroupIds) : null;
        return pedestal != null;
    }

    /**
     * Same as {@link #isCardAcceptedAt(Long, CampingCard)} for the pedestal code used on the pedestal link.
     */
    @Transactional(readOnly = true)
    public boolean isCardAcceptedAt(final String pedestalCode, final CampingCard card) {
        if (card == null || card.getGuest() == null) {
            return false;
        }
        Group issuerGroup = card.getGuest().getGroup();
        List<Long> allGroupIds = GroupServiceHelper.extractAllAcceptingSiteGroupIds(issuerGroup);
        Pedestal pedestal = (allGroupIds != null && !allGroupIds.isEmpty())
                ? pedestalDao.findByCodeAndGroupIds(pedestalCode, allGroupIds) : null;
        return pedestal != null;
    }

    /*NON_TRANSACTIONAL*/
    public List<Group> findManageableGroups(Group group) {
        Objects.requireNonNull(group);
        return groupDao.findManageableGroups(group.getId());
    }

    /*NON_TRANSACTIONAL*/
    public List<Long> findManageable(Group group) {
        Objects.requireNonNull(group);
        return groupDao.findManageable(group.getId());
    }

    /*NON_TRANSACTIONAL*/
    public List<Group> findParentGroups(Group group) {
        Objects.requireNonNull(group);
        return groupDao.findParentGroups(group.getId());
    }

    /*NON_TRANSACTIONAL*/
    public List<Long> findParents(Group group) {
        Objects.requireNonNull(group);
        return groupDao.findParents(group.getId());
    }

    /*NON_TRANSACTIONAL*/
    public MarketExtension<Market> getExtensionByMarket(final Market market) {
        return new MarketExtension<>(market, this);
    }

    @Transactional
    public Group createSiteGroup(Group parentGroup, String name, String displayName) {
        return groupManager.createChildGroup(parentGroup, name, displayName, GroupType.SITE);
    }

    @Transactional
    public Group createIssuerGroup(Group parentGroup, String name, String displayName) {
        return groupManager.createChildGroup(parentGroup, name, displayName, GroupType.ISSUER);
    }

    @Transactional
    public Group createStaffGroup(Group parentGroup, String name, String displayName) {
        return groupManager.createChildGroup(parentGroup, name, displayName, GroupType.STAFF);
    }
}
