package io.pillopl.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationTest {

    @Test
    void addressLineEndsWithTheCountryWhenItIsKnown() {
        Location location = new Location("Rovinj", "52210", "Obala", "12");

        assertThat(location.toAddressLine()).isEqualTo("Obala 12, 52210 Rovinj");

        location.setCountryIso("HR");
        assertThat(location.toAddressLine()).isEqualTo("Obala 12, 52210 Rovinj, HR");
    }

    @Test
    void onlyTheLocationNamedUnknownIsTheSupplierStockLocation() {
        Location location = new Location("Rovinj", "52210", "Obala", "12");
        assertThat(location.isUnknown()).isFalse();

        location.setName(Location.UNKNOWN);
        assertThat(location.isUnknown()).isTrue();
    }
}
