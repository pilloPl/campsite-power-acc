package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalListState;

import java.util.List;

/**
 * Hand-written pedestal SQL from the SQL map, restricted to the SITE subtree of a user.
 */
public interface PedestalDaoCustom {

    /**
     * @return the pedestal with the code when it hangs in the subtree of the user's SITE group, otherwise null
     */
    Pedestal findByCodeRestrictedToUserSiteGroup(String pedestalCode, Long userSiteGroupId);

    /**
     * Pedestals in the user's SITE subtree where the cards of the guest's ISSUER group are accepted
     * (one agreement semantics: the nearest non-empty agreement list replaces the lists above it).
     */
    List<Pedestal> findPedestalsForGuestRestrictedToUserSiteGroup(Long userSiteGroupId, Long guestIssuerGroupId);

    /**
     * Page of pedestals in a state, ordered by code; all of them when {@code rowsPerPage} is null.
     */
    List<Pedestal> findPedestalsByState(PedestalListState state, Long userSiteGroupId, Integer page,
                                        Integer rowsPerPage);

    long countPedestalsByState(PedestalListState state, Long userSiteGroupId);

    /**
     * Soft deletes (or restores) all pedestals of the groups except the given ones and sets their app listing.
     */
    int updatePedestalsDeletedByGroupIds(boolean deleted, boolean listedInApp, List<Long> groupIds,
                                         List<Long> excludeIds);
}
