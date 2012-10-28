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
}
