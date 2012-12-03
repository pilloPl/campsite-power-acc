package io.pillopl.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Walks over the loaded group trees in memory (no DAO access here, no transactions).
 */
public final class GroupServiceHelper {

    public static class GroupNameComparator implements Comparator<Group>, Serializable {

        @Override
        public int compare(Group o1, Group o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }

            String name1 = o1 != null ? o1.getName() : null;
            String name2 = o2 != null ? o2.getName() : null;

            if (name1 == null && name2 == null) {
                return 0;
            }
            if (name1 != null && name2 == null) {
                return 1;
            }
            if (name1 == null) {
                return -1;
            }
            return name1.compareTo(name2);
        }
    }

    public static List<Long> extractAllManageableGroupIDsForGroup(Group group, String searchString) {
        List<Long> result = new ArrayList<>();
        if (group != null) {
            addGroupIdIfMatches(result, group, searchString);
        }

        if (group == null || group.getChildren() == null || group.getChildren().isEmpty()) {
            return result;
        }

        addAllChildrenIDs(result, group, searchString);

        return result;
    }

    public static List<Group> extractAllManageableGroupsForGroup(Group group) {
        List<Group> result = new ArrayList<>();
        if (group == null) {
            return result;
        }

        result.add(group);

        if (group.getChildren() == null || group.getChildren().isEmpty()) {
            return result;
        }

        addAllChildrenGroups(result, group);

        return result;
    }

    public static Group getGroupByName(String groupName, List<Group> groups) {
        if (groupName != null && !groupName.isBlank() && groups != null) {
            for (Group group : groups) {
                if (groupName.equals(group.getName())) {
                    return group;
                }
            }
        }
        return null;
    }

    /**
     * Type hierarchy check: a group must have the type of its parent.
     */
    public static void checkParent(Group g, List<String> errors, Set<Long> checked) {
        Group parent = g.getParent();
        // found the top of the hierarchy
        if (parent == null) {
            return;
        }

        checked.add(g.getId());

        if (!g.getType().equals(parent.getType())) {
            errors.add(String.format("Type hierarchy broken in group tree: %s under %s", groupToPrettyString(g),
                    groupToPrettyString(parent)));
        }

        if (!checked.contains(parent.getId())) {
            checkParent(parent, errors, checked);
        }
    }

    public static String groupToPrettyString(Group group) {
        if (group == null) {
            return "Argument 'group' is null, nothing to pretty up!";
        }
        return concatWithDelim(", with TYPE:", group.getName(), group.getType());
    }

    public static String concat(final String s, final String... ss) {
        return concatWithDelim(" ", s, ss);
    }

    private static void addAllChildrenGroups(List<Group> list, Group group) {
        if (group == null || group.getChildren() == null || group.getChildren().isEmpty()) {
            return;
        }

        for (Group child : group.getChildren()) {
            if (!list.contains(child)) {
                list.add(child);
            }
            addAllChildrenGroups(list, child);
        }
    }

    private static void addAllChildrenIDs(List<Long> list, Group group, String searchString) {
        if (group == null || group.getChildren() == null || group.getChildren().isEmpty()) {
            return;
        }

        for (Group child : group.getChildren()) {
            if (!list.contains(child.getId())) {
                addGroupIdIfMatches(list, child, searchString);
            }
            addAllChildrenIDs(list, child, searchString);
        }
    }

    private static void addGroupIdIfMatches(List<Long> result, Group group, String searchString) {
        if (searchString == null || searchString.isEmpty()
                || group.getDisplayName().toUpperCase().contains(searchString.toUpperCase())) {
            result.add(group.getId());
        }
    }

    private static String concatWithDelim(final String delim, final String s, final String... ss) {
        if (ss == null) {
            return s;
        }

        final StringBuilder sBuilder = new StringBuilder(s);
        for (final String i_ss : ss) {
            sBuilder.append(delim).append(i_ss);
        }
        return sBuilder.toString();
    }

    private GroupServiceHelper() {
        throw new AssertionError();
    }
}
