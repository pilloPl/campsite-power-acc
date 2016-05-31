package io.pillopl.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Walks over the loaded group trees in memory (no DAO access here, no transactions).
 * <p>
 * The acceptance agreement rule implemented here is the only one in the system and is the same as the SQL
 * {@code findAcceptingGroupIds}: the nearest ancestor-or-self with a non-empty agreement list replaces the lists
 * of all groups above it.
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

    /**
     * Agreement list → subtrees → ids.
     *
     * @return ids of all SITE groups where the cards of the ISSUER group are accepted
     */
    public static List<Long> extractAllAcceptingSiteGroupIds(Group issuerGroup) {
        return extractAllAcceptingGroupIds(issuerGroup, GroupType.SITE);
    }

    public static List<Long> extractAllAcceptingGroupIds(Group group, GroupType targetGroupType) {
        List<Group> agreements = getAcceptanceAgreements(group, targetGroupType);
        if (agreements.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> allAcceptingGroupIds = new ArrayList<>();
        for (Group agreement : agreements) {
            for (Long id : collectSubtreeIds(agreement, null)) {
                if (!allAcceptingGroupIds.contains(id)) {
                    allAcceptingGroupIds.add(id);
                }
            }
        }
        return allAcceptingGroupIds;
    }

    public static List<Group> extractAllAcceptingGroups(Group group, GroupType targetGroupType) {
        List<Group> agreements = getAcceptanceAgreements(group, targetGroupType);
        if (agreements.isEmpty()) {
            return Collections.emptyList();
        }
        List<Group> allAcceptingGroups = new ArrayList<>();
        for (Group agreement : agreements) {
            for (Group accepting : collectSubtree(agreement)) {
                if (!allAcceptingGroups.contains(accepting)) {
                    allAcceptingGroups.add(accepting);
                }
            }
        }
        return allAcceptingGroups;
    }

    /**
     * Recursion up the parent line: the nearest ancestor-or-self whose agreement list contains groups of the target
     * type owns the list; lists above it are replaced, not merged. The type is checked on every element of the list
     * (elements of another type are not agreements of this kind and are ignored).
     */
    public static List<Group> getAcceptanceAgreements(Group group, GroupType targetGroupType) {
        if (group == null) {
            return Collections.emptyList();
        }

        List<Group> agreements = new ArrayList<>();
        if (group.getGroups() != null) {
            for (Group associatedGroup : group.getGroups()) {
                if (targetGroupType.isGroupType(associatedGroup)) {
                    agreements.add(associatedGroup);
                }
            }
        }
        if (!agreements.isEmpty()) {
            return agreements;
        }

        Group parent = group.getParent();
        if (parent == null) {
            return Collections.emptyList();
        }

        return getAcceptanceAgreements(parent, targetGroupType);
    }

    public static List<Long> collectSubtreeIds(Group group, String searchString) {
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

    public static List<Group> collectSubtree(Group group) {
        List<Group> result = new ArrayList<>();
        if (group == null) {
            return result;
        }

        result.add(group);

        if (group.getChildren() == null || group.getChildren().isEmpty()) {
            return result;
        }

        addDescendants(result, group);

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

    public static List<String> getGroupMarkets() {
        List<String> list = new ArrayList<>();
        for (Market market : Market.values()) {
            if (market.isPrefixMarket()) {
                list.add(market.name());
            }
        }
        return list;
    }

    public static String getAffiliateGroupName(String username) {
        return concat(GroupServiceConstants.PREFIX_AFFILIATE, username);
    }

    public static String getSupplierGroupName(String manufacturer) {
        return concat(GroupServiceConstants.PREFIX_SUPPLIER, manufacturer);
    }

    /**
     * Checks whether the group name starts with a market prefix ("HR ") and passes the market back.
     *
     * @return the market or null when the name has no market prefix
     */
    public static Market checkMarket(Group group) {
        for (Market market : Market.values()) {
            if (market.isPrefixMarket() && group != null && group.getName() != null
                    && group.getName().startsWith(market.name() + " ")) {
                return market;
            }
        }
        return null;
    }

    /**
     * Type hierarchy check: a group must have the type of its parent.
     */
    public static void checkParent(Group g, List<String> errors, Set<Long> checked) {
        Group parent = g.getParent();
        // reached the root
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

    /**
     * Agreement check: an ISSUER group may only list SITE groups, other groups no agreements at all.
     */
    public static void checkAgreements(Group g, List<String> errors) {
        if (g.getGroups() == null) {
            return;
        }
        for (Group associatedGroup : g.getGroups()) {
            if (!GroupType.ISSUER.isGroupType(g) || !GroupType.SITE.isGroupType(associatedGroup)) {
                errors.add(String.format("Acceptance agreement %s -> %s is not ISSUER -> SITE",
                        groupToPrettyString(g), groupToPrettyString(associatedGroup)));
            }
        }
    }

    public static String groupToPrettyString(Group group) {
        if (group == null) {
            return "<no group>";
        }
        return concatWithDelim(", with TYPE:", group.getName(), group.getType());
    }

    public static String concat(final String s, final String... ss) {
        return concatWithDelim(" ", s, ss);
    }

    private static void addDescendants(List<Group> list, Group group) {
        if (group == null || group.getChildren() == null || group.getChildren().isEmpty()) {
            return;
        }

        for (Group child : group.getChildren()) {
            if (!list.contains(child)) {
                list.add(child);
            }
            addDescendants(list, child);
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
