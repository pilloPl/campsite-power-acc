package io.pillopl.webservice.console.group;

import io.pillopl.group.AccessMode;
import io.pillopl.group.AffiliateGroupService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.GROUP;

    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired AffiliateGroupService affiliateGroupService;

    Group bay;

    @BeforeEach
    void setUp() {
        console.hrAdmin("admin");
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY CAMPSITE", "Bay", GroupType.SITE);
        console.user("reception", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"),
                bay);
    }

    @Test
    void siteTreeOfTheScopeIsReturnedWithItsChildren() throws Exception {
        call("admin", PATH + "/getGroupTree", new GetGroupTreeRequest("SITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tree.name").value("HR SITES"))
                .andExpect(jsonPath("$.tree.children[*].name").value(hasItem("BAY CAMPSITE")))
                .andExpect(jsonPath("$.tree.children[*].name").value(hasItem("NETWORK ACCESS HR")));
    }

    @Test
    void manageableGroupsAreFilteredByDisplayName() throws Exception {
        call("admin", PATH + "/findManageableGroups", new FindManageableGroupsRequest("SITE", "bay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups", hasSize(1)))
                .andExpect(jsonPath("$.groups[0].name").value("BAY CAMPSITE"));
        call("reception", PATH + "/findManageableGroups", new FindManageableGroupsRequest("ISSUER", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups", hasSize(0)));
    }

    @Test
    void groupIsCreatedOnceUnderAManageableParent() throws Exception {
        CreateGroupRequest request = new CreateGroupRequest(bay.getId(), "BAY SECTION A", null);

        call("admin", PATH + "/createGroup", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group.type").value("SITE"))
                .andExpect(jsonPath("$.group.displayName").value("BAY SECTION A"))
                .andExpect(jsonPath("$.group.parentId").value(bay.getId()));
        call("admin", PATH + "/createGroup", request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void groupOutsideTheScopeOrWithoutTheRightIsRefused() throws Exception {
        Long itSites = console.site("IT SITES").getId();

        call("admin", PATH + "/createGroup", new CreateGroupRequest(itSites, "ROMA", "Roma"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_AUTHORIZED"));
        call("reception", PATH + "/createGroup", new CreateGroupRequest(bay.getId(), "BAY SECTION A", null))
                .andExpect(status().isForbidden());
        call("nobody", PATH + "/createGroup", new CreateGroupRequest(bay.getId(), "BAY SECTION A", null))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongValuesAndUnreadableRequestsAreWrongRequests() throws Exception {
        call("admin", PATH + "/createGroup", new CreateGroupRequest(bay.getId(), "HR SITES | BAY", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("WRONG_REQUEST"));
        call("admin", PATH + "/getGroupTree", new GetGroupTreeRequest("CAMPSITE"))
                .andExpect(status().isBadRequest());
        call("admin", PATH + "/getGroupAttributes", new GetGroupAttributesRequest(999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_EXISTS"));
        callRaw("admin", PATH + "/getGroupTree", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("WRONG_REQUEST"));
    }

    @Test
    void agreementOfAParentIsInheritedUntilItIsRemoved() throws Exception {
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "CARAVAN CLUB",
                "Caravan club", GroupType.ISSUER);
        Group clubYouth = groupManager.createChildGroup(club, "CARAVAN CLUB YOUTH", "Youth", GroupType.ISSUER);

        call("admin", PATH + "/addAcceptanceAgreement", new AddAcceptanceAgreementRequest(club.getId(), bay.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreement.inherited").value(false))
                .andExpect(jsonPath("$.agreement.acceptingSiteGroupIds", hasItem(bay.getId().intValue())));
        call("admin", PATH + "/findAcceptanceAgreements", new FindAcceptanceAgreementsRequest(clubYouth.getId()))
                .andExpect(jsonPath("$.agreement.inherited").value(true))
                .andExpect(jsonPath("$.agreement.agreements[0].name").value("BAY CAMPSITE"));

        call("admin", PATH + "/removeAcceptanceAgreement",
                new RemoveAcceptanceAgreementRequest(club.getId(), bay.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreement.agreements", hasSize(0)));
        call("admin", PATH + "/removeAcceptanceAgreement",
                new RemoveAcceptanceAgreementRequest(club.getId(), bay.getId()))
                .andExpect(status().isNotFound());
        call("admin", PATH + "/addAcceptanceAgreement", new AddAcceptanceAgreementRequest(bay.getId(), club.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAffiliateCampsitesSwitchTheirAccessMode() throws Exception {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.HOUSE_CARDS_ONLY);

        call("admin", PATH + "/switchAccessMode",
                new SwitchAccessModeRequest(affiliate.getId(), "network access", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessMode").value("NETWORK_ACCESS"));
        call("admin", PATH + "/switchAccessMode", new SwitchAccessModeRequest(bay.getId(), "NETWORK_ACCESS", true))
                .andExpect(status().isConflict());
    }

    @Test
    void effectiveAttributesShowMarketAndAccessMode() throws Exception {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.HOUSE_CARDS_ONLY);

        call("admin", PATH + "/getGroupAttributes", new GetGroupAttributesRequest(affiliate.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes.market").value("HR"))
                .andExpect(jsonPath("$.attributes.accessMode").value("HOUSE_CARDS_ONLY"))
                .andExpect(jsonPath("$.attributes.exportCampsiteName").value("Marko"))
                .andExpect(jsonPath("$.attributes.supplierStock").value(false));
        call("admin", PATH + "/getGroupAttributes", new GetGroupAttributesRequest(bay.getId()))
                .andExpect(jsonPath("$.attributes.accessMode").doesNotExist());
    }

    @Test
    void groupValidityCheckNeedsTheConfigurationRight() throws Exception {
        call("admin", PATH + "/checkGroupValidity", new CheckGroupValidityRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").isBoolean());
        call("reception", PATH + "/checkGroupValidity", new CheckGroupValidityRequest())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(not("")));
    }
}
