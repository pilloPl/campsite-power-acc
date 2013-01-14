package io.pillopl.group;

import io.pillopl.persistence.dao.GroupDao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Creates child groups and acceptance agreements. Duplicate names within a type are refused by the database
 * (UNIQUE(NAME, TYPENAME)).
 */
@Component
public class GroupManager {

    private final GroupDao groupDao;

    public GroupManager(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

    @Transactional
    public Group createRootGroup(String name, String displayName, GroupType groupType) {
        return groupDao.saveAndFlush(new Group(groupType, name, displayName));
    }

    @Transactional
    public Group createChildGroup(Group parent, String name, String displayName, GroupType groupType) {
        Assert.isTrue(parent.getType().equals(groupType.name()), "Group type mismatch.");
        return createChildGroup(parent, name, displayName);
    }

    @Transactional
    public Group createChildGroup(Group parent, String name, String displayName) {
        Group child = new Group();
        child.setType(parent.getType());
        child.setName(name);
        child.setDisplayName(displayName);
        child.setParent(parent);

        child = groupDao.saveAndFlush(child);

        parent.addChild(child);

        return child;
    }

    /**
     * Cards of the issuer group are accepted in the subtree of the site group. The agreement list of the issuer
     * group replaces the lists of its ancestors.
     */
    @Transactional
    public Group addAcceptanceAgreement(Group issuerGroup, Group siteGroup) {
        Assert.isTrue(GroupType.ISSUER.isGroupType(issuerGroup), "Agreement owner must be an ISSUER group.");
        Assert.isTrue(GroupType.SITE.isGroupType(siteGroup), "Agreement target must be a SITE group.");
        issuerGroup.addAssociatedGroup(siteGroup);
        return groupDao.saveAndFlush(issuerGroup);
    }

    @Transactional
    public Group removeAcceptanceAgreement(Group issuerGroup, Group siteGroup) {
        issuerGroup.getGroups().remove(siteGroup);
        return groupDao.saveAndFlush(issuerGroup);
    }
}
