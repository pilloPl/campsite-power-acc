package io.pillopl.util;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.user.Userinfo;

/**
 * The SITE subtree a console call looks at: the caller's SITE group, or one campsite group of that subtree.
 */
public final class SiteScopeHelper {

    private SiteScopeHelper() {
        throw new AssertionError();
    }

    /**
     * @param campsiteGroupId a SITE group of the caller's subtree, null for the caller's whole SITE scope
     * @return the root of the subtree to read, null when the caller has no SITE scope (nothing to see)
     * @throws AccessNotAllowedException when the campsite group lies outside the caller's subtree
     */
    public static Long siteGroupIdOf(Userinfo principal, Long campsiteGroupId, GroupService groupService) {
        Group siteGroup = principal.getGroup(GroupType.SITE);
        if (siteGroup == null) {
            if (campsiteGroupId != null) {
                throw outOfScope(principal, campsiteGroupId);
            }
            return null;
        }
        if (campsiteGroupId == null) {
            return siteGroup.getId();
        }
        if (!groupService.findManageable(siteGroup).contains(campsiteGroupId)) {
            throw outOfScope(principal, campsiteGroupId);
        }
        return campsiteGroupId;
    }

    private static AccessNotAllowedException outOfScope(Userinfo principal, Long campsiteGroupId) {
        return new AccessNotAllowedException("Campsite group " + campsiteGroupId + " is not in the scope of "
                + principal.getUsername());
    }
}
