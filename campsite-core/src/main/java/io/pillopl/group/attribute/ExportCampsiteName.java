package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Campsite name used in exports (issuer exchange, reports) instead of the group display name.
 */
@Entity
@Table(name = "GRP_ATTR_EXPORT_CAMPSITE_NAME")
@Getter
@Setter
public class ExportCampsiteName extends GroupAttribute {

    @Column(name = "NAME", nullable = false, length = 256)
    private String name;

    public ExportCampsiteName() {
    }

    public ExportCampsiteName(String name) {
        this.name = name;
    }

    /**
     * The attribute's name, else the display name of the group, else the default.
     */
    public static String computeName(ExportCampsiteName exportCampsiteName, Group group, String defaultName) {
        return (null != exportCampsiteName) ? exportCampsiteName.getName()
                : (null == group) ? defaultName : group.getDisplayName();
    }
}
