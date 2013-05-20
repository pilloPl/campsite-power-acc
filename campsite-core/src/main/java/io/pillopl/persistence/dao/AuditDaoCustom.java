package io.pillopl.persistence.dao;

public interface AuditDaoCustom {

    /**
     * Deletes the audit rows older than their expiry period (days, per category and success / error).
     *
     * @return number of deleted rows
     */
    int cleanAuditTable(int technicalSuccessExpiryInDays, int technicalErrorExpiryInDays,
                        int functionalSuccessExpiryInDays, int functionalErrorExpiryInDays);
}
