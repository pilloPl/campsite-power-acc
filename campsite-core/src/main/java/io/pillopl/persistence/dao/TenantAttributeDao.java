package io.pillopl.persistence.dao;

import io.pillopl.group.attribute.IssuerTenant;
import io.pillopl.group.attribute.SiteTenant;

/**
 * Nearest tenant on the parent line in one recursive SQL (ORDER BY level).
 */
public interface TenantAttributeDao {

    SiteTenant findNearestSiteTenant(Long groupId);

    IssuerTenant findNearestIssuerTenant(Long groupId);
}
