package io.pillopl.user;

/**
 * What changed on a user (attribute of the user changed event).
 */
public enum UserinfoChangeType {
    CREATED,
    EMAIL_CHANGED,
    NAME_CHANGED,
    AFFILIATE_OWNER_SETUP,
    PASSWORD_CHANGED,
    STATUS_CHANGED,
    ROLES_CHANGED
}
