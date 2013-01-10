package io.pillopl.group;

import java.util.List;

/**
 * Market dependent group names and lookups of the groups found by those names. Not a bean: created by
 * {@link GroupService#getExtensionByMarket(Market)}; lookups run in the caller's transaction.
 */
public class MarketExtension<M extends Enum<Market>> {

    private final Market market;

    private final GroupService groupService;

    MarketExtension(final Market market, final GroupService groupService) {
        this.market = market;
        this.groupService = groupService;
    }

    public Market getType() {
        return market;
    }

    public Group findIssuerRoot() {
        return groupService.fetchIssuerGroup(getIssuerRootName());
    }

    public Group findSiteRoot() {
        return groupService.fetchSiteGroup(getSiteRootName());
    }

    public Group findStaffRoot() {
        return groupService.fetchStaffGroup(getStaffRootName());
    }

    public Group findNetworkAccessSiteGroup() {
        return groupService.fetchSiteGroup(getNetworkAccessGroupName());
    }

    public Group findHouseCardsOnlySiteGroup() {
        return groupService.fetchSiteGroup(getHouseCardsOnlyGroupName());
    }

    public Group findAccessModeSiteGroup(AccessMode accessMode) {
        return accessMode == AccessMode.NETWORK_ACCESS ? findNetworkAccessSiteGroup() : findHouseCardsOnlySiteGroup();
    }

    public Group findWelcomeCardPoolIssuerGroup() {
        List<Group> groups = GroupServiceHelper.extractAllManageableGroupsForGroup(findIssuerRoot());
        return GroupServiceHelper.getGroupByName(getWelcomeCardPoolGroupName(), groups);
    }

    public Group findReceptionStaffGroup() {
        List<Group> groups = GroupServiceHelper.extractAllManageableGroupsForGroup(findStaffRoot());
        return GroupServiceHelper.getGroupByName(getReceptionGroupName(), groups);
    }

    public String getIssuerRootName() {
        return GroupServiceHelper.concat(market.name(), GroupServiceConstants.ISSUERS);
    }

    public String getSiteRootName() {
        return GroupServiceHelper.concat(market.name(), GroupServiceConstants.SITES);
    }

    public String getStaffRootName() {
        return GroupServiceHelper.concat(market.name(), GroupServiceConstants.STAFF);
    }

    public String getNetworkAccessGroupName() {
        return GroupServiceHelper.concat(GroupServiceConstants.NETWORK_ACCESS, market.name());
    }

    public String getHouseCardsOnlyGroupName() {
        return GroupServiceHelper.concat(GroupServiceConstants.HOUSE_CARDS_ONLY, market.name());
    }

    public String getWelcomeCardPoolGroupName() {
        return GroupServiceHelper.concat(market.name(), GroupServiceConstants.WELCOME_CARD_POOL);
    }

    public String getReceptionGroupName() {
        return GroupServiceHelper.concat(market.name(), GroupServiceConstants.RECEPTION);
    }

    public String getRootName(GroupType groupType) {
        return switch (groupType) {
            case ISSUER -> getIssuerRootName();
            case SITE -> getSiteRootName();
            case STAFF -> getStaffRootName();
        };
    }
}
