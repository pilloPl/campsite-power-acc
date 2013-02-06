package io.pillopl.group.attribute;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhitelistFilterTypeTest {

    @Test
    void whitelistFilterTypesCoverTheirDirections() {
        assertThat(WhitelistFilterType.isImportWhitelistFilterType(WhitelistFilterType.IMPORT)).isTrue();
        assertThat(WhitelistFilterType.isImportWhitelistFilterType(WhitelistFilterType.EXPORT)).isFalse();
        assertThat(WhitelistFilterType.isExportWhitelistFilterType(WhitelistFilterType.IMPORT_EXPORT)).isTrue();
        assertThat(WhitelistFilterType.isExportWhitelistFilterType(WhitelistFilterType.IMPORT)).isFalse();
        assertThat(WhitelistFilterType.isExportWhitelistFilterType(null)).isFalse();
    }

    @Test
    void missingFilterTypeMeansBothDirections() {
        assertThat(WhitelistFilterType.fromString(null)).isEqualTo(WhitelistFilterType.IMPORT_EXPORT);
        assertThat(WhitelistFilterType.fromString("EXPORT")).isEqualTo(WhitelistFilterType.EXPORT);
    }
}
