package io.pillopl.group.attribute;

import java.util.List;
import java.util.function.Predicate;

/**
 * The one resolution rule of group attributes: nearest ancestor-or-self wins.
 */
public final class GroupAttributeHelper {

    /**
     * @param parentLineIds group ids ordered from the group itself up to its root
     * @param attributes    attributes of any groups on that line, in any order
     * @return the attribute of the first group on the line that has one, or null
     */
    public static <A extends GroupAttribute> A findNearest(List<Long> parentLineIds, List<A> attributes) {
        return findNearest(parentLineIds, attributes, attribute -> true);
    }

    /**
     * Same as {@link #findNearest(List, List)}, considering only attributes that apply to the question asked
     * (e.g. a whitelist filter for the import direction).
     */
    public static <A extends GroupAttribute> A findNearest(List<Long> parentLineIds, List<A> attributes,
                                                           Predicate<? super A> applies) {
        if (parentLineIds == null || attributes == null) {
            return null;
        }
        for (Long groupId : parentLineIds) {
            for (A attribute : attributes) {
                if (attribute != null && groupId.equals(attribute.getGroupId()) && applies.test(attribute)) {
                    return attribute;
                }
            }
        }
        return null;
    }

    private GroupAttributeHelper() {
        throw new AssertionError();
    }
}
