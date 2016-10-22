package io.pillopl.issuer;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.card.CampingCard;
import io.pillopl.card.CardActivation;
import io.pillopl.card.CardFixture;
import io.pillopl.dto.admin.IssuerPartnerDTO;
import io.pillopl.dto.admin.IssuerSyncStatusDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoStatus;
import io.pillopl.user.UserroleName;
import io.pillopl.value.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The issuer exchange tab on the database: partners are created and changed by the configuration right, the sync
 * matrix lists their directions, a card is sent to the partners again.
 */
@Transactional
class IssuerPartnerConsoleIntegrationTest extends IntegrationTest {

    static final Username ADMIN = Username.of("issuer-admin");
    static final Username OFFICE = Username.of("issuer-office");

    @Autowired IssuerApiService issuerApi;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired CardFixture cards;
    @Autowired CampingCardDao campingCardDao;
    @Autowired UserinfoDao userinfoDao;
    @Autowired UserroleDao userroleDao;

    Group partnerCards;
    Group partnerSites;

    @BeforeEach
    void setUp() {
        partnerCards = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "LAKE CLUB",
                "Lake club");
        partnerSites = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "LAKE CLUB SITES",
                "Lake club campsites");
        user(ADMIN.value(), UserroleName.MARKET_ADMIN);
        user(OFFICE.value(), UserroleName.BACK_OFFICE);
    }

    @Test
    void administratorCreatesAndChangesAPartnerAndTheMatrixShowsItsDirections() {
        IssuerPartnerDTO created = issuerApi.saveIssuerPartner(ADMIN, partner(null, "LAKECLUB", true, false));

        assertThat(created.id()).isNotNull();
        assertThat(created.issuerGroupName()).isEqualTo("LAKE CLUB");
        assertThat(created.createDate()).isNotNull();

        IssuerPartnerDTO changed = issuerApi.saveIssuerPartner(ADMIN, new IssuerPartnerDTO(created.id(), "LAKECLUB",
                "Lake club", partnerCards.getId(), null, partnerSites.getId(), null, "V1", "/exchange/lakeclub",
                true, true, true, false, true, null));
        assertThat(changed.siteGroupName()).isEqualTo("LAKE CLUB SITES");
        assertThat(changed.interfaceVersion()).isEqualTo("V1");

        assertThat(issuerApi.findIssuerPartners(OFFICE)).extracting(IssuerPartnerDTO::name).contains("LAKECLUB");
        assertThat(issuerApi.findIssuerSyncStatuses(OFFICE)).filteredOn(s -> s.partnerName().equals("LAKECLUB"))
                .extracting(IssuerSyncStatusDTO::direction, IssuerSyncStatusDTO::status).containsExactly(
                        org.assertj.core.api.Assertions.tuple("CARD_IMPORT", "NOT_RUN"),
                        org.assertj.core.api.Assertions.tuple("CARD_EXPORT", "NOT_RUN"),
                        org.assertj.core.api.Assertions.tuple("RECORD_EXPORT", "NOT_RUN"));
    }

    @Test
    void partnerRulesAreCheckedBeforeSaving() {
        issuerApi.saveIssuerPartner(ADMIN, partner(null, "LAKECLUB", true, false));

        assertThatThrownBy(() -> issuerApi.saveIssuerPartner(ADMIN, partner(null, "LAKECLUB", true, false)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> issuerApi.saveIssuerPartner(ADMIN, partner(null, "RIVERCLUB", false, true)))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("SITE group");
        assertThatThrownBy(() -> issuerApi.saveIssuerPartner(ADMIN, new IssuerPartnerDTO(null, "RIVERCLUB", null,
                partnerCards.getId(), null, null, null, "V9", "https://river.example.org", true, false, false, false,
                true, null))).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> issuerApi.saveIssuerPartner(ADMIN, new IssuerPartnerDTO(987_654L, "RIVERCLUB", null,
                partnerCards.getId(), null, null, null, "V2", "https://river.example.org", true, false, false, false,
                true, null))).isInstanceOf(ObjectNotFoundException.class);
        assertThatThrownBy(() -> issuerApi.saveIssuerPartner(OFFICE, partner(null, "RIVERCLUB", true, false)))
                .isInstanceOf(AccessNotAllowedException.class);
    }

    @Test
    void cardIsSentToThePartnersAgainUnlessItIsKeptOutOrHasNothingToSend() {
        Guest guest = cards.guest("Kralj", groupService.fetchNetworkCardIssuerGroup());
        CampingCard exported = cards.card("7801", "N7801", guest, LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(3));
        exported.setTargetPartnerSyncState(PartnerSyncState.ADD);
        exported.setPartnerSyncState(PartnerSyncState.NOT);
        campingCardDao.saveAndFlush(exported);

        assertThat(issuerApi.resyncCard(OFFICE, exported.getId()).partnerSyncState()).isEqualTo("MODIFY");

        CampingCard neverExportedBlocked = cards.card("7802", "N7802", guest, null, null);
        neverExportedBlocked.setActivation(CardActivation.DEACTIVATED);
        neverExportedBlocked.setTargetPartnerSyncState(null);
        campingCardDao.saveAndFlush(neverExportedBlocked);
        assertThatThrownBy(() -> issuerApi.resyncCard(OFFICE, neverExportedBlocked.getId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("nothing to send");

        CampingCard keptOut = cards.card("7803", "N7803", guest, null, null);
        keptOut.setPartnerSyncState(PartnerSyncState.NEVER);
        campingCardDao.saveAndFlush(keptOut);
        assertThatThrownBy(() -> issuerApi.resyncCard(OFFICE, keptOut.getId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("kept out");
        assertThatThrownBy(() -> issuerApi.resyncCard(OFFICE, 987_654_321L))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    private IssuerPartnerDTO partner(Long id, String name, boolean cardImport, boolean cardExport) {
        return new IssuerPartnerDTO(id, name, null, partnerCards.getId(), null, null, null, "V2",
                "https://" + name.toLowerCase() + ".example.org", cardImport, cardExport, false, false, true, null);
    }

    private void user(String username, UserroleName role) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setStatus(UserinfoStatus.ACTIVE);
        user.setGroup(groupService.fetchStaffGroup("HR STAFF"));
        user.getGroups().add(groupService.fetchIssuerGroup("HR ISSUERS"));
        user.getRoles().add(userroleDao.findByName(role.name()));
        userinfoDao.saveAndFlush(user);
    }
}
