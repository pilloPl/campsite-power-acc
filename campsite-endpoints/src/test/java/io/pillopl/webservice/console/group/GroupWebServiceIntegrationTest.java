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
        call(PATH + "/getGroupTree", new GetGroupTreeRequest("admin", "SITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tree.name").value("HR SITES"))
                .andExpect(jsonPath("$.tree.children[*].name").value(hasItem("BAY CAMPSITE")))
                .andExpect(jsonPath("$.tree.children[*].name").value(hasItem("NETWORK ACCESS HR")));
    }

    @Test
    void manageableGroupsAreFilteredByDisplayName() throws Exception {
        call(PATH + "/findManageableGroups", new FindManageableGroupsRequest("admin", "SITE", "bay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups", hasSize(1)))
                .andExpect(jsonPath("$.groups[0].name").value("BAY CAMPSITE"));
        call(PATH + "/findManageableGroups", new FindManageableGroupsRequest("reception", "ISSUER", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups", hasSize(0)));
    }

    @Test
    void groupIsCreatedOnceUnderAManageableParent() throws Exception {
        CreateGroupRequest request = new CreateGroupRequest("admin", bay.getId(), "BAY SECTION A", null);

        call(PATH + "/createGroup", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group.type").value("SITE"))
                .andExpect(jsonPath("$.group.displayName").value("BAY SECTION A"))
                .andExpect(jsonPath("$.group.parentId").value(bay.getId()));
        call(PATH + "/createGroup", request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void groupOutsideTheScopeOrWithoutTheRightIsRefused() throws Exception {
        Long itSites = console.site("IT SITES").getId();

        call(PATH + "/createGroup", new CreateGroupRequest("admin", itSites, "ROMA", "Roma"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_AUTHORIZED"));
        call(PATH + "/createGroup", new CreateGroupRequest("reception", bay.getId(), "BAY SECTION A", null))
                .andExpect(status().isForbidden());
        call(PATH + "/createGroup", new CreateGroupRequest("nobody", bay.getId(), "BAY SECTION A", null))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongValuesAndUnreadableRequestsAreWrongRequests() throws Exception {
        call(PATH + "/createGroup", new CreateGroupRequest("admin", bay.getId(), "HR SITES | BAY", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("WRONG_REQUEST"));
        call(PATH + "/getGroupTree", new GetGroupTreeRequest("admin", "CAMPSITE"))
                .andExpect(status().isBadRequest());
        call(PATH + "/getGroupAttributes", new GetGroupAttributesRequest("admin", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_EXISTS"));
        callRaw(PATH + "/getGroupTree", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("WRONG_REQUEST"));
    }

    @Test
    void agreementOfAParentIsInheritedUntilItIsRemoved() throws Exception {
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "CARAVAN CLUB",
                "Caravan club", GroupType.ISSUER);
        Group clubYouth = groupManager.createChildGroup(club, "CARAVAN CLUB YOUTH", "Youth", GroupType.ISSUER);

        call(PATH + "/addAcceptanceAgreement", new AddAcceptanceAgreementRequest("admin", club.getId(), bay.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreement.inherited").value(false))
                .andExpect(jsonPath("$.agreement.acceptingSiteGroupIds", hasItem(bay.getId().intValue())));
        call(PATH + "/findAcceptanceAgreements", new FindAcceptanceAgreementsRequest("admin", clubYouth.getId()))
                .andExpect(jsonPath("$.agreement.inherited").value(true))
                .andExpect(jsonPath("$.agreement.agreements[0].name").value("BAY CAMPSITE"));

        call(PATH + "/removeAcceptanceAgreement",
                new RemoveAcceptanceAgreementRequest("admin", club.getId(), bay.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreement.agreements", hasSize(0)));
        call(PATH + "/removeAcceptanceAgreement",
                new RemoveAcceptanceAgreementRequest("admin", club.getId(), bay.getId()))
                .andExpect(status().isNotFound());
        call(PATH + "/addAcceptanceAgreement", new AddAcceptanceAgreementRequest("admin", bay.getId(), club.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAffiliateCampsitesSwitchTheirAccessMode() throws Exception {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.HOUSE_CARDS_ONLY);

        call(PATH + "/switchAccessMode",
                new SwitchAccessModeRequest("admin", affiliate.getId(), "network access", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessMode").value("NETWORK_ACCESS"));
        call(PATH + "/switchAccessMode", new SwitchAccessModeRequest("admin", bay.getId(), "NETWORK_ACCESS", true))
                .andExpect(status().isConflict());
    }

    @Test
    void effectiveAttributesShowMarketAndAccessMode() throws Exception {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.HOUSE_CARDS_ONLY);

        call(PATH + "/getGroupAttributes", new GetGroupAttributesRequest("admin", affiliate.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes.market").value("HR"))
                .andExpect(jsonPath("$.attributes.accessMode").value("HOUSE_CARDS_ONLY"))
                .andExpect(jsonPath("$.attributes.exportCampsiteName").value("Marko"))
                .andExpect(jsonPath("$.attributes.supplierStock").value(false));
        call(PATH + "/getGroupAttributes", new GetGroupAttributesRequest("admin", bay.getId()))
                .andExpect(jsonPath("$.attributes.accessMode").doesNotExist());
    }

    @Test
    void groupValidityCheckNeedsTheConfigurationRight() throws Exception {
        call(PATH + "/checkGroupValidity", new CheckGroupValidityRequest("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").isBoolean());
        call(PATH + "/checkGroupValidity", new CheckGroupValidityRequest("reception"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(not("")));
    }
}
