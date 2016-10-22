package io.pillopl.issuer;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.dto.admin.IssuerSyncStatusDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuerPartnerTest {

    final Group cards = new Group(GroupType.ISSUER, "SEA CLUB");
    final Group sites = new Group(GroupType.SITE, "SEA CLUB SITES");

    @Test
    void partnerWithNameEndpointAndIssuerGroupIsValid() {
        IssuerPartner partner = partner();
        partner.setCardImport(true);
        partner.setRecordExport(true);

        partner.validate();

        assertThat(partner.getDirections()).containsExactly(IssuerSyncStatus.Direction.CARD_IMPORT,
                IssuerSyncStatus.Direction.RECORD_EXPORT);
    }

    @Test
    void cardExportAndRecordImportNeedTheSiteGroupOfThePartner() {
        IssuerPartner partner = partner();
        partner.setCardExport(true);
        assertThatThrownBy(partner::validate).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SITE group");

        partner.setSiteGroup(sites);
        partner.setRecordImport(true);
        partner.validate();
        assertThat(partner.getDirections()).containsExactly(IssuerSyncStatus.Direction.CARD_EXPORT,
                IssuerSyncStatus.Direction.RECORD_IMPORT);
    }

    @Test
    void groupsMustBeOfTheirTreesAndNameAndEndpointPresent() {
        IssuerPartner wrongIssuer = partner();
        wrongIssuer.setIssuerGroup(sites);
        assertThatThrownBy(wrongIssuer::validate).isInstanceOf(IllegalArgumentException.class);

        IssuerPartner wrongSite = partner();
        wrongSite.setSiteGroup(cards);
        assertThatThrownBy(wrongSite::validate).isInstanceOf(IllegalArgumentException.class);

        IssuerPartner noName = partner();
        noName.setName(" ");
        assertThatThrownBy(noName::validate).isInstanceOf(IllegalArgumentException.class);

        IssuerPartner noEndpoint = partner();
        noEndpoint.setEndpoint(null);
        assertThatThrownBy(noEndpoint::validate).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void syncMatrixShowsEveryDirectionOfThePartnerAndNotRunForTheNewOnes() {
        IssuerPartner partner = partner();
        partner.setCardImport(true);
        partner.setRecordExport(true);
        partner.getSyncStatus(IssuerSyncStatus.Direction.CARD_IMPORT).succeeded(LocalDateTime.of(2026, 7, 10, 2, 0),
                12, 1, null);
        partner.getSyncStatus(IssuerSyncStatus.Direction.RECORD_IMPORT); // switched off: not in the matrix

        assertThat(IssuerApiService.syncStatusMatrixRow(partner)).extracting(IssuerSyncStatusDTO::direction,
                IssuerSyncStatusDTO::status, IssuerSyncStatusDTO::itemsAccepted).containsExactly(
                org.assertj.core.api.Assertions.tuple("CARD_IMPORT", "OK", 12),
                org.assertj.core.api.Assertions.tuple("RECORD_EXPORT", "NOT_RUN", 0));
    }

    private IssuerPartner partner() {
        return new IssuerPartner("SEACLUB", cards, IssuerPartner.InterfaceVersion.V2, "https://partner.example.org");
    }
}
