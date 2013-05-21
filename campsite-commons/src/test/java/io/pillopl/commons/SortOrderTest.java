package io.pillopl.commons;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SortOrderTest {

    @Test
    void directionIsReadLenientlyWithDescendingAsDefault() {
        assertThat(SortDirection.fromString("asc", SortDirection.DESC)).isEqualTo(SortDirection.ASC);
        assertThat(SortDirection.fromString("Descending", SortDirection.ASC)).isEqualTo(SortDirection.DESC);
        assertThat(SortDirection.fromString("sideways", SortDirection.DESC)).isEqualTo(SortDirection.DESC);
        assertThat(SortDirection.fromString(null, SortDirection.ASC)).isEqualTo(SortDirection.ASC);
    }

    @Test
    void blankColumnAndDirectionMeanUnsorted() {
        assertThat(SortOrder.of(" ", null)).isEqualTo(SortOrder.UNSORTED);
        assertThat(SortOrder.of(null, null).isSorted()).isFalse();
        assertThat(SortOrder.of(" actionDate ", "asc")).isEqualTo(new SortOrder("actionDate", SortDirection.ASC));
    }
}
