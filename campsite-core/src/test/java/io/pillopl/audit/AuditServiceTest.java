package io.pillopl.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditServiceTest {

    @Test
    void longTextIsAbbreviatedToTheColumnLength() {
        String abbreviated = AuditService.abbreviate("x".repeat(150), 100);

        assertThat(abbreviated).hasSize(100).endsWith("...");
    }

    @Test
    void shortOrMissingTextIsKept() {
        assertThat(AuditService.abbreviate("ok", 100)).isEqualTo("ok");
        assertThat(AuditService.abbreviate(null, 100)).isNull();
    }
}
