package io.pillopl.group;

import io.pillopl.dto.group.GroupTreeNodeDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the tree transfer objects of the console's tree diagrams from the in-memory group tree (children ordered by
 * id). Only groups of the type of the root are taken; a group met twice (a broken tree) is not expanded again.
 */
public final class GroupTreeFactory {

    /**
     * Deeper trees are cut; the real trees have about five levels (network, market, campsite, section).
     */
    public static final int MAX_DEPTH = 20;

    private GroupTreeFactory() {
        throw new AssertionError();
    }

    public static GroupTreeNodeDTO createTree(Group root) {
        if (root == null) {
            return null;
        }
        return createNode(root, root.getType(), new HashSet<>(), 0);
    }

    private static GroupTreeNodeDTO createNode(Group group, String type, Set<Long> visited, int depth) {
        visited.add(group.getId());
        List<GroupTreeNodeDTO> children = new ArrayList<>();
        if (depth < MAX_DEPTH && group.getChildren() != null) {
            for (Group child : group.getChildren()) {
                if (type.equals(child.getType()) && !visited.contains(child.getId())) {
                    children.add(createNode(child, type, visited, depth + 1));
                }
            }
        }
        return new GroupTreeNodeDTO(group.getId(), group.getName(), group.getDisplayName(), group.getType(),
                children);
    }

    /**
     * @return number of nodes of the tree
     */
    public static int count(GroupTreeNodeDTO node) {
        if (node == null) {
            return 0;
        }
        int count = 1;
        for (GroupTreeNodeDTO child : node.children()) {
            count += count(child);
        }
        return count;
    }
}
