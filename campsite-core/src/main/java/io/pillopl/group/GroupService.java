package io.pillopl.group;

import io.pillopl.persistence.dao.GroupDao;
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
 * prefixes.
 */
@Service
public class GroupService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupService.class);

    private final GroupDao groupDao;

    private final GroupManager groupManager;

    public GroupService(GroupDao groupDao, GroupManager groupManager) {
        this.groupDao = groupDao;
        this.groupManager = groupManager;
    }

    /**
     * Consistency check of the trees, run by an integration test and by the nightly control job.
     * <p>
     * Today it checks the type hierarchy. Next to come (same loop shape): staff users must hang
     * in STAFF groups, guests in ISSUER groups, pedestals in SITE groups.
     *
     * @return true when no error was found (errors are logged)
     */
    @Transactional(readOnly = true)
    public boolean checkAllGroupsForValidity() {
        Set<Long> checked = new HashSet<>();
        List<String> errors = new ArrayList<>();
        List<Group> all = groupDao.findAll();

        // check for valid type-hierarchy
        for (Group g : all) {
            GroupServiceHelper.checkParent(g, errors, checked);
        }

        for (String error : errors) {
            LOG.error(error);
        }
        return errors.isEmpty();
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
    public List<Group> fetchAllGroupsByType(GroupType type) {
        List<Group> result = new ArrayList<>();
        for (Group root : groupDao.findRoots(type.name())) {
            result.addAll(GroupServiceHelper.extractAllManageableGroupsForGroup(root));
        }
        return result;
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
