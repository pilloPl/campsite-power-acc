package io.pillopl.infrastructure.supplier;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierAccessibilityTest {

    @Test
    void accessibilityIsReadIgnoringCaseAndBlanks() {
        assertThat(SupplierAccessibility.parse(" public paying ")).isEqualTo(SupplierAccessibility.PUBLIC_PAYING);
        assertThat(SupplierAccessibility.parse("Restricted")).isEqualTo(SupplierAccessibility.RESTRICTED);
    }

    @Test
    void blankOrUnknownAccessibilityIsUnspecified() {
        assertThat(SupplierAccessibility.parse(null)).isEqualTo(SupplierAccessibility.UNSPECIFIED);
        assertThat(SupplierAccessibility.parse("  ")).isEqualTo(SupplierAccessibility.UNSPECIFIED);
        assertThat(SupplierAccessibility.parse("members only")).isEqualTo(SupplierAccessibility.UNSPECIFIED);
    }

    @Test
    void accessibilityDoesNotDependOnTheDefaultLocale() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(SupplierAccessibility.parse("public paying")).isEqualTo(SupplierAccessibility.PUBLIC_PAYING);
        } finally {
            Locale.setDefault(before);
        }
    }
}
