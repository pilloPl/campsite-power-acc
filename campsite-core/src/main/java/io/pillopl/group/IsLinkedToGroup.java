package io.pillopl.group;

/**
 * An object hanging in one of the group trees: a pedestal in a SITE group, a guest in an ISSUER group,
 * a staff user in a STAFF group.
 */
public interface IsLinkedToGroup {

    Group getGroup();

    void setGroup(Group group);
}
