package io.pillopl.audit;

import io.pillopl.persistence.dao.AuditDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not transactional: audit rows are written in their own transaction.
 */
class AuditIntegrationTest extends IntegrationTest {

    @Autowired AuditService auditService;
    @Autowired AuditDao auditDao;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM AUDIT");
    }

    @Test
    void sentMailIsAuditedAsFunctionalSuccess() {
        auditService.auditSendEmail("owner@example.org", "Pedestal back to supplier", "body", null);

        assertThat(auditService.getAuditEntries(null, "EMAIL", null, AuditCategory.FUNCTIONAL, null, null, null, null,
                null)).singleElement().satisfies(a -> {
            assertThat(a.getStatus()).isEqualTo("SUCCESS");
            assertThat(a.getInfoField1()).isEqualTo("Pedestal back to supplier");
            assertThat(a.getManagedServer()).isNotBlank();
        });
    }

    @Test
    void failedMailIsAuditedWithItsReason() {
        auditService.auditSendEmail("x" + "y".repeat(200), "s", "body", "SMTP down");

        assertThat(auditService.getAuditEntries(null, null, "error", null, null, null, "smtp", "actionDate", "asc"))
                .singleElement().satisfies(a -> assertThat(a.getActionName()).hasSize(100));
    }

    @Test
    void searchFiltersByDateAndFreeText() {
        auditService.auditAction(AuditActionType.PEDESTAL_COMMISSIONING, "ivo", "ACME-0001", "1042", "ok", null);
        LocalDateTime now = LocalDateTime.now();

        assertThat(auditService.getAuditEntries(null, null, null, null, now.minusHours(1), now.plusHours(1), "IVO",
                "no-such-column", "sideways")).hasSize(1);
        assertThat(auditService.getAuditEntries(null, null, null, null, now.plusHours(1), null, null, null, null))
                .isEmpty();
    }

    @Test
    void expiredRowsAreDeletedByCategoryAndStatus() {
        LocalDateTime old = LocalDateTime.now().minusDays(400);
        insert(AuditCategory.TECHNICAL, "SUCCESS", old);
        insert(AuditCategory.FUNCTIONAL, "ERROR", old);
        insert(AuditCategory.FUNCTIONAL, "SUCCESS", LocalDateTime.now());

        int deleted = auditDao.cleanAuditTable(30, 365, 30, 365);

        assertThat(deleted).isEqualTo(2);
        assertThat(auditDao.count()).isEqualTo(1);
    }

    @Test
    void errorRowsAreKeptLongerThanSuccessRows() {
        LocalDateTime twoMonthsAgo = LocalDateTime.now().minusDays(60);
        insert(AuditCategory.TECHNICAL, "SUCCESS", twoMonthsAgo);
        insert(AuditCategory.TECHNICAL, "ERROR", twoMonthsAgo);

        assertThat(auditDao.cleanAuditTable(30, 90, 30, 90)).isEqualTo(1);
        assertThat(auditDao.findAll()).singleElement().extracting(Audit::getStatus).isEqualTo("ERROR");
    }

    private void insert(AuditCategory category, String status, LocalDateTime actionDate) {
        Audit audit = new Audit();
        audit.setCategory(category);
        audit.setStatus(status);
        audit.setActionDate(actionDate);
        auditService.insert(audit);
    }
}
