package io.pillopl.admin;

import io.pillopl.audit.Audit;
import io.pillopl.config.SystemProperty;
import io.pillopl.config.SystemPropertyKey;
import io.pillopl.dto.admin.AuditEntryDTO;
import io.pillopl.dto.admin.SystemPropertyDTO;
import io.pillopl.dto.user.UserroleDTO;
import io.pillopl.user.Userright;
import io.pillopl.user.Userrole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Audit rows, configuration and roles to transfer objects.
 */
public final class AdministrationMapper {

    private AdministrationMapper() {
        throw new AssertionError();
    }

    public static AuditEntryDTO toDTO(Audit audit) {
        return new AuditEntryDTO(audit.getId(), audit.getCategory() != null ? audit.getCategory().name() : null,
                audit.getUserId(), audit.getActionDate(), audit.getActionType(), audit.getActionName(),
                audit.getActionGroupId(), audit.getStatus(), audit.getErrorReason(), audit.getMessage());
    }

    public static List<AuditEntryDTO> toAuditDTOs(List<Audit> audits) {
        List<AuditEntryDTO> result = new ArrayList<>(audits.size());
        for (Audit audit : audits) {
            result.add(toDTO(audit));
        }
        return result;
    }

    /**
     * One entry per key in the order of the enum; keys without a row have no value (the coded default applies).
     */
    public static List<SystemPropertyDTO> toSystemPropertyDTOs(Collection<SystemProperty> rows) {
        Map<SystemPropertyKey, SystemProperty> byKey = new EnumMap<>(SystemPropertyKey.class);
        for (SystemProperty row : rows) {
            byKey.put(row.getKey(), row);
        }
        List<SystemPropertyDTO> result = new ArrayList<>();
        for (SystemPropertyKey key : SystemPropertyKey.values()) {
            SystemProperty row = byKey.get(key);
            result.add(row != null ? toDTO(row) : new SystemPropertyDTO(key.name(), null, null));
        }
        return result;
    }

    public static SystemPropertyDTO toDTO(SystemProperty row) {
        return new SystemPropertyDTO(row.getKey().name(), row.getValue(), row.getDescription());
    }

    /**
     * Rights sorted by name, group types as configured.
     */
    public static UserroleDTO toDTO(Userrole role) {
        List<String> groupTypes = new ArrayList<>();
        if (role.getGroupTypes() != null && !role.getGroupTypes().isBlank()) {
            for (String type : role.getGroupTypes().split(",")) {
                groupTypes.add(type.trim());
            }
        }
        TreeSet<String> rights = new TreeSet<>();
        for (Userright right : role.getRights()) {
            rights.add(right.getName());
        }
        return new UserroleDTO(role.getName(), groupTypes, role.isSelfService(), new ArrayList<>(rights));
    }

    public static List<UserroleDTO> toRoleDTOs(List<Userrole> roles) {
        List<UserroleDTO> result = new ArrayList<>(roles.size());
        for (Userrole role : roles) {
            result.add(toDTO(role));
        }
        return result;
    }
}
