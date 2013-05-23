package io.pillopl.dto.user;

import java.io.Serializable;

/**
 * One row of the self-service user list (materialized view USERINFO_SUMMARY).
 */
public record UserinfoSummaryDTO(Long id, String username, String name, String email, String status, Long groupId,
                                 boolean affiliateOwner, boolean seasonalResident) implements Serializable {
}
