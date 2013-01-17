package io.pillopl.persistence.dao;

import io.pillopl.group.Group;

import java.util.List;

/**
 * Hand-written recursive SQL over the group trees (statements from the SQL map).
 */
public interface GroupDaoCustom {

    /**
     * @return ids of the subtree including the group itself, ordered hierarchically
     */
    List<Long> findManageable(Long groupId);

    List<Group> findManageableGroups(Long groupId);

    /**
     * @return ids of the parent line from the group itself up to its root
     */
    List<Long> findParents(Long groupId);

    List<Group> findParentGroups(Long groupId);

    /**
     * Groups of {@code targetType} accepting the group (e.g. SITE groups accepting cards of an ISSUER group):
     * the subtrees of the nearest ancestor-or-self agreement list (that list replaces the lists above it).
     */
    List<Long> findAcceptingGroupIds(Long groupId, String targetType);

    /**
     * Reverse of {@link #findAcceptingGroupIds(Long, String)}, same semantics: groups of {@code sourceType} whose
     * effective agreement list covers the given group.
     */
    List<Long> findGroupIdsAcceptedAt(Long groupId, String sourceType, String targetType);
}
