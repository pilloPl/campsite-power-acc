package io.pillopl.audit;

import io.pillopl.commons.TextUtil;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.persistence.dao.AuditDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Audit trail: writing never throws (callers always proceed), search for the console, expiry.
 */
@Service
public class AuditService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    private static final int PAGE_SIZE = 1000;

    private static final Set<String> SORT_COLUMNS = Set.of("id", "dateCreated", "actionDate", "actionType",
            "actionName", "status", "category", "userId");

    private final AuditDao auditDao;

    private final SystemPropertyService systemPropertyService;

    private final TransactionTemplate requiresNew;

    public AuditService(AuditDao auditDao, SystemPropertyService systemPropertyService,
                        PlatformTransactionManager transactionManager) {
        this.auditDao = auditDao;
        this.systemPropertyService = systemPropertyService;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * First page (1000 rows) of the audit search; unknown sort columns fall back to id, the order to descending.
     */
    @Transactional(readOnly = true)
    public List<Audit> getAuditEntries(String nameFilter, String typeFilter, String statusFilter,
                                       AuditCategory categoryFilter, LocalDateTime dateFrom, LocalDateTime dateTo,
                                       String freeSearch, String sortColumnName, String sortOrder) {
        LOG.debug(String.format("Getting audit entries [name:'%s', type:'%s', status:'%s', from:'%s', to:'%s', "
                        + "free:'%s', sort:'%s' %s]", nameFilter, typeFilter, statusFilter, dateFrom, dateTo,
                freeSearch, sortColumnName, sortOrder));

        String column = sortColumnName;
        if (column == null || !SORT_COLUMNS.contains(column)) {
            column = "id";
        }

        PageRequest pageRequest = PageRequest.of(0, PAGE_SIZE, Sort.by(sortDirection(sortOrder), column));
        return auditDao.findAuditEntries(blankToNull(nameFilter), blankToNull(typeFilter), blankToNull(statusFilter),
                categoryFilter, dateFrom, dateTo, blankToNull(freeSearch), pageRequest);
    }

    /**
     * Descending unless the console asks for a known order; an unknown order is logged.
     */
    static Sort.Direction sortDirection(String sortOrder) {
        if (sortOrder == null || sortOrder.isBlank()) {
            return Sort.Direction.DESC;
        }
        try {
            return Sort.Direction.fromString(sortOrder.trim());
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown sort order '" + sortOrder + "' of the audit search, sorting descending");
            return Sort.Direction.DESC;
        }
    }

    /**
     * Audits a sent (or failed) mail. Never throws, so that client code can always proceed.
     */
    public void auditSendEmail(String recipients, String subject, String emailMessage, String exceptionMessage) {
        try {
            LOG.debug("auditSendEmail " + recipients);

            Audit audit = new Audit();
            audit.setActionDate(LocalDateTime.now());
            audit.setActionName(recipients);
            audit.setActionType(AuditActionType.EMAIL.name());
            audit.setCategory(AuditCategory.FUNCTIONAL);
            audit.setInfoField1(subject);
            audit.setAuditData(emailMessage);

            if (exceptionMessage != null) {
                audit.setErrorReason(exceptionMessage);
                audit.setStatus(Audit.ERROR);
            } else {
                audit.setStatus(Audit.SUCCESS);
            }
            insert(audit);
        } catch (RuntimeException e) {
            LOG.warn("Failed to audit EMAIL: " + e.getMessage(), e);
        }
    }

    /**
     * Audits a functional action of a user (e.g. commissioning). Never throws.
     */
    public void auditAction(AuditActionType type, String userId, String actionName, String groupId, String message,
                            String errorReason) {
        try {
            Audit audit = new Audit();
            audit.setActionDate(LocalDateTime.now());
            audit.setCategory(AuditCategory.FUNCTIONAL);
            audit.setActionType(type.name());
            audit.setUserId(userId);
            audit.setActionName(actionName);
            audit.setActionGroupId(groupId);
            audit.setMessage(message);
            audit.setErrorReason(errorReason);
            audit.setStatus(errorReason == null ? Audit.SUCCESS : Audit.ERROR);
            insert(audit);
        } catch (RuntimeException e) {
            LOG.warn("Failed to audit " + type + ": " + e.getMessage(), e);
        }
    }

    /**
     * Inserts a row in its own transaction (it stays even when the caller rolls back). Abbreviates the free text
     * columns to their lengths.
     */
    public Long insert(Audit audit) {
        audit.setUserId(abbreviate(audit.getUserId(), 100));
        audit.setActionName(abbreviate(audit.getActionName(), 100));
        audit.setActionType(abbreviate(audit.getActionType(), 50));
        audit.setActionGroupId(abbreviate(audit.getActionGroupId(), 50));
        audit.setStatus(abbreviate(audit.getStatus(), 15));
        audit.setErrorReason(abbreviate(audit.getErrorReason(), 1900));
        if (audit.getManagedServer() == null) {
            audit.setManagedServer(hostName());
        }
        audit.setManagedServer(abbreviate(audit.getManagedServer(), 50));
        audit.setInfoField1(abbreviate(audit.getInfoField1(), 200));
        audit.setInfoField2(abbreviate(audit.getInfoField2(), 200));
        audit.setMessage(abbreviate(audit.getMessage(), 2000));
        audit.setAuditData(abbreviate(audit.getAuditData(), 2000));
        return requiresNew.execute(status -> auditDao.save(audit).getId());
    }

    /**
     * Deletes expired rows: success rows after AuditSuccessEventsExpiryPeriodInDays, the others after
     * AuditErrorEventsExpiryPeriodInDays, for both categories.
     */
    @Transactional
    public int cleanAuditTable() {
        int success = systemPropertyService.getAuditSuccessEventsExpiryPeriodInDays();
        int error = systemPropertyService.getAuditErrorEventsExpiryPeriodInDays();
        int deleted = auditDao.cleanAuditTable(success, error, success, error);
        LOG.info(String.format("Deleted %d expired audit rows", deleted));
        return deleted;
    }

    static String abbreviate(String value, int maxLength) {
        return TextUtil.abbreviate(value, maxLength);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
