package io.pillopl.audit;

/**
 * What was audited (column ACTION_TYPE).
 */
public enum AuditActionType {
    EMAIL,
    PEDESTAL_IMPORT,
    PEDESTAL_COMMISSIONING,
    PEDESTAL_DECOMMISSIONING,
    SOCKET_OUT_OF_SERVICE,
    CARD_IMPORT,
    ERP_EXPORT,
    CONFIG_CHANGE,
    LOGIN,
    CONSOLE_ACTION
}
