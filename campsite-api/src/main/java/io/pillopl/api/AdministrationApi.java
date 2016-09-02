package io.pillopl.api;

import io.pillopl.commons.SortOrder;
import io.pillopl.dto.admin.AuditEntryDTO;
import io.pillopl.dto.admin.AuditFilterDTO;
import io.pillopl.dto.admin.RightsMatrixDTO;
import io.pillopl.dto.admin.SystemPropertyDTO;
import io.pillopl.dto.user.UserroleDTO;
import io.pillopl.value.Username;

import java.util.List;

/**
 * Administration: audit log, configuration in the database, roles and rights, materialized views.
 */
public interface AdministrationApi {

    List<AuditEntryDTO> findAuditEntries(Username authUsername, AuditFilterDTO filter, SortOrder sortOrder);

    List<SystemPropertyDTO> getSystemProperties(Username authUsername);

    SystemPropertyDTO setSystemProperty(Username authUsername, String key, String value, String description);

    List<UserroleDTO> findRoles(Username authUsername);

    void refreshUserSummary(Username authUsername);

    /**
     * All rights, every role with its rights and the security groups with their roles. USER_ADMIN.
     */
    RightsMatrixDTO getRightsMatrix(Username authUsername);

    /**
     * Reads SYSTEM_PROPERTY again (after changes in the database) and answers the values now in effect.
     * CONFIG_EDIT.
     */
    List<SystemPropertyDTO> reloadConfiguration(Username authUsername);

    /**
     * Audit row of a state-changing console call (never throws): who called which operation with which request, and
     * the error when the call failed.
     */
    void auditConsoleAction(String username, String operation, String request, String errorReason);
}
