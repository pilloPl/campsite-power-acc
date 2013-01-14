package io.pillopl.group;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Builds detached group trees with ids for unit tests of the in-memory walks.
 */
class InMemoryGroups {

    private final AtomicLong ids = new AtomicLong(1);

    Group root(GroupType type, String name) {
        Group group = new Group(type, name);
        group.setId(ids.getAndIncrement());
        return group;
    }

    Group child(Group parent, String name) {
        Group group = new Group(GroupType.valueOf(parent.getType()), name);
        group.setId(ids.getAndIncrement());
        group.setParent(parent);
        parent.addChild(group);
        return group;
    }

    static void agree(Group issuerGroup, Group... siteGroups) {
        for (Group siteGroup : siteGroups) {
            issuerGroup.addAssociatedGroup(siteGroup);
        }
    }
}
