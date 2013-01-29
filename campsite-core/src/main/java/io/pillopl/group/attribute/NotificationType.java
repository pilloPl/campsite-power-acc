package io.pillopl.group.attribute;

/**
 * Kinds of service mails sent to the address configured on a SITE subtree; each names its mail template.
 */
public enum NotificationType {

    /**
     * Fault and status messages for the pedestal service partner.
     */
    PEDESTAL_STATUS("mail/pedestal-status.ftl"),

    FIRMWARE_STATUS("mail/pedestal-firmware-status.ftl"),

    DIAGNOSTICS_STATUS("mail/pedestal-diagnostics-status.ftl");

    private final String template;

    NotificationType(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }
}
