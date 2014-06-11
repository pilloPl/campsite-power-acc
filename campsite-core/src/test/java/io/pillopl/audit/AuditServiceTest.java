package io.pillopl.audit;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class AuditServiceTest {

    @Test
    void longTextIsAbbreviatedToTheColumnLength() {
        String abbreviated = AuditService.abbreviate("x".repeat(150), 100);

        assertThat(abbreviated).hasSize(100).endsWith("...");
    }

    @Test
    void auditSearchSortsDescendingUnlessAKnownOrderIsAsked() {
        assertThat(AuditService.sortDirection(" asc ")).isEqualTo(Sort.Direction.ASC);
        assertThat(AuditService.sortDirection("DESC")).isEqualTo(Sort.Direction.DESC);
        assertThat(AuditService.sortDirection("sideways")).isEqualTo(Sort.Direction.DESC);
        assertThat(AuditService.sortDirection(null)).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shortOrMissingTextIsKept() {
        assertThat(AuditService.abbreviate("ok", 100)).isEqualTo("ok");
        assertThat(AuditService.abbreviate(null, 100)).isNull();
    }
}
