package io.pillopl.group.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Mail recipient for one {@link NotificationType} in a SITE subtree (at most one per group and type).
 */
@Entity
@Table(name = "GRP_ATTR_NOTIFICATION")
@Getter
@Setter
public class Notification extends GroupAttribute {

    @Column(name = "NAME", nullable = false, length = 256)
    private String name;

    @Column(name = "EMAIL", nullable = false, length = 256)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "NOTIFICATION_TYPE", nullable = false, length = 64)
    private NotificationType notificationType;

    public Notification() {
    }

    public Notification(String name, String email, NotificationType notificationType) {
        this.name = name;
        this.email = email;
        this.notificationType = notificationType;
    }
}
