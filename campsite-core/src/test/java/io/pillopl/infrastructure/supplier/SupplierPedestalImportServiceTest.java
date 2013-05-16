package io.pillopl.infrastructure.supplier;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierPedestalImportServiceTest {

    private final Group campsite = new Group(GroupType.SITE, "BAY CAMPSITE");
    private final Group houseCardsOnly = new Group(GroupType.SITE, "HOUSE CARDS ONLY HR");
    private final Group networkAccess = new Group(GroupType.SITE, "NETWORK ACCESS HR");

    @Test
    void publicPedestalsHangUnderNetworkAccess() {
        assertThat(SupplierPedestalImportService.determineGroup(campsite, houseCardsOnly, networkAccess,
                SupplierAccessibility.PUBLIC_PAYING)).isSameAs(networkAccess);
        assertThat(SupplierPedestalImportService.determineGroup(campsite, houseCardsOnly, networkAccess,
                SupplierAccessibility.PUBLIC_FREE)).isSameAs(networkAccess);
    }

    @Test
    void restrictedPedestalsHangUnderHouseCardsOnly() {
        assertThat(SupplierPedestalImportService.determineGroup(campsite, houseCardsOnly, networkAccess,
                SupplierAccessibility.RESTRICTED)).isSameAs(houseCardsOnly);
    }

    @Test
    void pedestalWithoutAccessInformationStaysInTheGivenGroup() {
        assertThat(SupplierPedestalImportService.determineGroup(campsite, houseCardsOnly, networkAccess,
                SupplierAccessibility.UNSPECIFIED)).isSameAs(campsite);
    }

    @Test
    void unknownAccessibilityInTheFileIsUnspecified() {
        assertThat(SupplierAccessibility.parse("public paying")).isEqualTo(SupplierAccessibility.PUBLIC_PAYING);
        assertThat(SupplierAccessibility.parse("members")).isEqualTo(SupplierAccessibility.UNSPECIFIED);
        assertThat(SupplierAccessibility.parse(null)).isEqualTo(SupplierAccessibility.UNSPECIFIED);
    }
}
