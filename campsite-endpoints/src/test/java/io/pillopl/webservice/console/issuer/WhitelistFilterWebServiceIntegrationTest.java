package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.WhitelistFilterDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.group.attribute.WhitelistFilterService;
import io.pillopl.issuer.IssuerPartner;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Whitelist filters of the issuer exchange tab: which card issuer groups take part in the exchange with the partners.
 */
class WhitelistFilterWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.ISSUER;

    @Autowired GroupManager groupManager;
    @Autowired IssuerPartnerDao issuerPartnerDao;
    @Autowired WhitelistFilterService whitelistFilterService;

    Group club;
    Group itClub;
    IssuerPartner partner;

    @BeforeEach
    void setUp() {
        club = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "FILTER CLUB", "Filter club",
                GroupType.ISSUER);
        itClub = groupManager.createChildGroup(console.issuer("IT ISSUERS"), "IT FILTER CLUB", "IT club",
                GroupType.ISSUER);
        partner = issuerPartnerDao.saveAndFlush(new IssuerPartner("FILTER PARTNER", club,
                IssuerPartner.InterfaceVersion.V2, "https://filter.example.org"));
        console.hrAdmin("admin");
        console.user("office", UserroleName.BACK_OFFICE, console.staff("HR STAFF"), console.issuer("HR ISSUERS"));
    }

    @Test
    void administratorKeepsAClubOutOfTheCardImportOfOnePartnerAndChangesAndRemovesTheFilter() throws Exception {
        call("admin", PATH + "/saveWhitelistFilter", new SaveWhitelistFilterRequest(new WhitelistFilterDTO(null,
                club.getId(), null, null, false, "IMPORT", partner.getId(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter.groupName").value("FILTER CLUB"))
                .andExpect(jsonPath("$.filter.filterType").value("IMPORT"))
                .andExpect(jsonPath("$.filter.issuerPartnerName").value("FILTER PARTNER"));
        assertThat(whitelistFilterService.isSynchronizableForImport(club.getId(), partner)).isFalse();
        assertThat(whitelistFilterService.isSynchronizableForExport(club.getId(), partner)).isTrue();

        call("office", PATH + "/findWhitelistFilters", new FindWhitelistFiltersRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters[?(@.groupName == 'FILTER CLUB')].synchronizable").value(
                        org.hamcrest.Matchers.contains(false)));
        Long filterId = whitelistFilterService.findFiltersOfSubtree(club).get(0).getId();

        call("admin", PATH + "/saveWhitelistFilter", new SaveWhitelistFilterRequest(new WhitelistFilterDTO(filterId,
                club.getId(), null, null, false, "IMPORT_EXPORT", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter.id").value(filterId))
                .andExpect(jsonPath("$.filter.issuerPartnerId").isEmpty());
        assertThat(whitelistFilterService.isSynchronizableForExport(club.getId(), partner)).isFalse();

        call("admin", PATH + "/deleteWhitelistFilter", new DeleteWhitelistFilterRequest(filterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedFilterId").value(filterId));
        assertThat(whitelistFilterService.isSynchronizableForImport(club.getId(), partner)).isTrue();
        assertThat(jdbcTemplate.queryForList("SELECT ACTION_NAME FROM AUDIT WHERE ACTION_TYPE = 'CONSOLE_ACTION' "
                + "AND USERID = 'admin' ORDER BY ID", String.class)).containsExactly("issuer/saveWhitelistFilter",
                "issuer/saveWhitelistFilter", "issuer/deleteWhitelistFilter");
    }

    @Test
    void filtersAreSetOnIssuerGroupsInTheScopeByCallersWithTheConfigurationRight() throws Exception {
        call("office", PATH + "/saveWhitelistFilter", new SaveWhitelistFilterRequest(new WhitelistFilterDTO(null,
                club.getId(), null, null, false, "IMPORT", null, null))).andExpect(status().isForbidden());
        call("admin", PATH + "/saveWhitelistFilter", new SaveWhitelistFilterRequest(new WhitelistFilterDTO(null,
                itClub.getId(), null, null, false, "IMPORT", null, null))).andExpect(status().isForbidden());
        call("admin", PATH + "/saveWhitelistFilter", new SaveWhitelistFilterRequest(new WhitelistFilterDTO(null,
                console.site("HR SITES").getId(), null, null, false, "IMPORT", null, null)))
                .andExpect(status().isBadRequest());
        call("admin", PATH + "/saveWhitelistFilter", new SaveWhitelistFilterRequest(new WhitelistFilterDTO(null,
                club.getId(), null, null, false, "SIDEWAYS", null, null))).andExpect(status().isBadRequest());
        call("admin", PATH + "/saveWhitelistFilter", new SaveWhitelistFilterRequest(new WhitelistFilterDTO(null,
                club.getId(), null, null, false, "IMPORT", 987_654_321L, null))).andExpect(status().isNotFound());
        call("admin", PATH + "/deleteWhitelistFilter", new DeleteWhitelistFilterRequest(987_654_321L))
                .andExpect(status().isNotFound());
        call("admin", PATH + "/findWhitelistFilters", new FindWhitelistFiltersRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters[?(@.groupName == 'FILTER CLUB')]", hasSize(0)));
    }
}
