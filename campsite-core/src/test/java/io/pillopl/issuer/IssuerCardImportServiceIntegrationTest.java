package io.pillopl.issuer;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardActivation;
import io.pillopl.card.CardFixture;
import io.pillopl.card.CardStatus;
import io.pillopl.card.RfidConverter;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.attribute.WhitelistFilter;
import io.pillopl.group.attribute.WhitelistFilterType;
import io.pillopl.guest.Guest;
import io.pillopl.issuer.IssuerExchange.CardLine;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.persistence.dao.WhitelistFilterDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class IssuerCardImportServiceIntegrationTest extends IntegrationTest {

    static final String UID = "04A1B2C3";
    static final String ACCOUNT = "770012345678";
    static final LocalDate MEMBERSHIP_END = LocalDate.of(2026, 12, 31);

    @Autowired IssuerCardImportService issuerCardImportService;
    @Autowired IssuerPartnerDao issuerPartnerDao;
    @Autowired CampingCardDao campingCardDao;
    @Autowired GuestDao guestDao;
    @Autowired WhitelistFilterDao whitelistFilterDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired CardFixture cards;

    Group clubGroup;
    IssuerPartner club;
    IssuerPartner federation;

    @BeforeEach
    void setUp() {
        Group hrIssuers = groupService.fetchIssuerGroup("HR ISSUERS");
        clubGroup = groupManager.createChildGroup(hrIssuers, "IMPORT CARAVAN CLUB", "Caravan club");
        Group federationGroup = groupManager.createChildGroup(hrIssuers, "IMPORT CAMPING FEDERATION", "Federation");
        club = partner("CARAVAN CLUB", clubGroup);
        federation = partner("CAMPING FEDERATION", federationGroup);
    }

    @Test
    void addedCardOfAnIssuerHangsOnThePartnersGuestWithoutBillingAccounts() {
        CampingCard card = issuerCardImportService.importCard(club, add(UID, "CC-1001"));

        assertThat(card.getCardRfid()).isEqualTo(RfidConverter.hexBigEndianToDecimal(UID));
        assertThat(card.getCardNumber()).isEqualTo("CC-1001");
        assertThat(card.getValidFrom()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(card.getValidTo()).isEqualTo(MEMBERSHIP_END);
        assertThat(card.getBillingAccountNo()).isEqualTo(ACCOUNT);
        Guest guest = guestDao.findById(card.getGuest().getId()).orElseThrow();
        assertThat(guest.getName()).isEqualTo("CARAVAN CLUB");
        assertThat(guest.getGroup().getId()).isEqualTo(clubGroup.getId());
        assertThat(guest.getBillingAccounts()).isEmpty();
        assertThat(card.validate(LocalDateTime.of(2026, 7, 10, 12, 0))).isEqualTo(CardStatus.VALID);
    }

    @Test
    void importedCardsNeverGoBackIntoTheCardListExport() {
        CampingCard card = issuerCardImportService.importCard(club, add(UID, "CC-1001"));

        assertThat(card.getPartnerSyncState()).isEqualTo(PartnerSyncState.NEVER);
        assertThat(card.getTargetPartnerSyncState()).isEqualTo(PartnerSyncState.NEVER);
    }

    @Test
    void secondCardOfThePartnerUsesTheSameGuest() {
        CampingCard first = issuerCardImportService.importCard(club, add(UID, "CC-1001"));
        CampingCard second = issuerCardImportService.importCard(club, add("04A1B2C4", "CC-1002"));

        assertThat(second.getGuest().getId()).isEqualTo(first.getGuest().getId());
    }

    @Test
    void addOfACardOfThePartnerReactivatesIt() {
        CampingCard card = issuerCardImportService.importCard(club, add(UID, "CC-1001"));
        issuerCardImportService.importCard(club, line(UID, "CC-1001", "DELETE", "2026-07-05 10:00"));

        CampingCard again = issuerCardImportService.importCard(club, line(UID, "CC-1001", "ADD", "2026-07-20 08:00"));

        assertThat(again.getId()).isEqualTo(card.getId());
        assertThat(again.getActivation()).isEqualTo(CardActivation.ACTIVATED);
        assertThat(again.getValidFrom()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void networkCardIsNeverTakenOverByAnIssuer() {
        Guest networkGuest = cards.guest("Horvat", groupService.fetchNetworkCardIssuerGroup());
        cards.card(RfidConverter.hexBigEndianToDecimal(UID), "HR-1", networkGuest, null, null);

        assertThatThrownBy(() -> issuerCardImportService.importCard(club, add(UID, "CC-1001")))
                .isInstanceOf(IssuerExchangeException.class)
                .hasMessageContaining("exists as a network card");
    }

    @Test
    void cardImportedFromAnotherIssuerIsRefused() {
        issuerCardImportService.importCard(federation, add(UID, "CF-1"));

        assertThatThrownBy(() -> issuerCardImportService.importCard(club, add(UID, "CC-1001")))
                .isInstanceOf(IssuerExchangeException.class)
                .hasMessageContaining("imported from another issuer");
        assertThatThrownBy(() -> issuerCardImportService.importCard(club, line(UID, "CF-1", "DELETE", null)))
                .hasMessageContaining("imported from another issuer");
    }

    @Test
    void issuerSwitchedOffByAWhitelistFilterIsNotImported() {
        WhitelistFilter filter = new WhitelistFilter(false, WhitelistFilterType.IMPORT);
        filter.setGroup(clubGroup);
        filter.setIssuerPartner(club);
        whitelistFilterDao.saveAndFlush(filter);

        assertThatThrownBy(() -> issuerCardImportService.importCard(club, add(UID, "CC-1001")))
                .isInstanceOf(IssuerExchangeException.class)
                .hasMessage("Issuer 'Caravan club' is marked as not synchronizable");
    }

    @Test
    void addNeedsAWellFormedBillingAccountOfTheIssuer() {
        CardLine line = new CardLine(UID, "CC-1001", "ADD", "2026-07-01 00:00", "ACC-1", null);

        assertThatThrownBy(() -> issuerCardImportService.importCard(club, line))
                .hasMessageContaining("billing account number 'ACC-1' is not valid");
    }

    @Test
    void modifyChangesTheNumberAndTheValidity() {
        issuerCardImportService.importCard(club, add(UID, "CC-1001"));

        CampingCard card = issuerCardImportService.importCard(club,
                new CardLine(UID, "CC-2001", "modify", "2026-08-01 00:00", ACCOUNT, LocalDate.of(2027, 3, 31)));

        assertThat(card.getCardNumber()).isEqualTo("CC-2001");
        assertThat(card.getValidFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(card.getValidTo()).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    @Test
    void cardOnItsWayOutCannotBeModified() {
        issuerCardImportService.importCard(club, add(UID, "CC-1001"));
        issuerCardImportService.importCard(club, line(UID, "CC-1001", "DELETE", null));

        assertThatThrownBy(() -> issuerCardImportService.importCard(club, line(UID, "CC-1001", "MODIFY", null)))
                .hasMessage("CARD_UID='04A1B2C3' is being deleted");
    }

    @Test
    void modifyAndDeleteOfAnUnknownCardAreRefused() {
        assertThatThrownBy(() -> issuerCardImportService.importCard(club, line(UID, "CC-1", "MODIFY", null)))
                .hasMessage("CARD_UID='04A1B2C3' card not found");
        assertThatThrownBy(() -> issuerCardImportService.importCard(club, line(UID, "CC-1", "DELETE", null)))
                .hasMessage("CARD_UID='04A1B2C3' card not found");
    }

    @Test
    void deleteDeactivatesTheCardFromTheActionDate() {
        issuerCardImportService.importCard(club, add(UID, "CC-1001"));

        CampingCard card = issuerCardImportService.importCard(club, line(UID, "CC-1001", "DELETE",
                "2026-09-30 18:00"));

        assertThat(card.getActivation()).isEqualTo(CardActivation.DEACTIVATED);
        assertThat(card.getValidFrom()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(card.getValidTo()).isNull();
    }

    @Test
    void unreadableLinesAreRefused() {
        assertThatThrownBy(() -> issuerCardImportService.importCard(club, line(UID, "CC-1", "RENEW", null)))
                .hasMessage("CARD_UID='04A1B2C3' unknown action code RENEW");
        assertThatThrownBy(() -> issuerCardImportService.importCard(club, line(UID, "CC-1", "ADD", "01.07.2026")))
                .hasMessage("CARD_UID='04A1B2C3' cannot parse the action date 01.07.2026");
        assertThatThrownBy(() -> issuerCardImportService.importCard(club, line("XYZ", "CC-1", "ADD", null)))
                .hasMessage("CARD_UID='XYZ' is no hexadecimal UID");
        assertThatThrownBy(() -> issuerCardImportService.importCard(club, line(" ", "CC-1", "ADD", null)))
                .hasMessage("Card UID is missing in the line of CARAVAN CLUB");
    }

    private static CardLine add(String uid, String number) {
        return new CardLine(uid, number, "ADD", "2026-07-01 09:30", ACCOUNT, MEMBERSHIP_END);
    }

    private static CardLine line(String uid, String number, String action, String date) {
        return new CardLine(uid, number, action, date, ACCOUNT, MEMBERSHIP_END);
    }

    private IssuerPartner partner(String name, Group issuerGroup) {
        IssuerPartner partner = new IssuerPartner(name, issuerGroup, IssuerPartner.InterfaceVersion.V1, "/tmp/x");
        partner.setCardImport(true);
        return issuerPartnerDao.saveAndFlush(partner);
    }
}
