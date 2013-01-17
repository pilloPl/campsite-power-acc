package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.TenantAttributeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the campsite operator (site tenant) of a SITE group: the nearest tenant on the parent line.
 */
@Service
public class SiteTenantService {

    private static final Logger LOG = LoggerFactory.getLogger(SiteTenantService.class);

    static final String NOTHING_FOUND = "N/A";

    private final TenantAttributeDao tenantAttributeDao;

    public SiteTenantService(TenantAttributeDao tenantAttributeDao) {
        this.tenantAttributeDao = tenantAttributeDao;
    }

    /**
     * @return the nearest {@link SiteTenant} or null when the group is null, not a SITE group or has none
     */
    @Transactional(readOnly = true)
    public SiteTenant findFor(Group group) {
        SiteTenant siteTenant = null;
        if (group != null && GroupType.SITE.isGroupType(group)) {
            try {
                siteTenant = tenantAttributeDao.findNearestSiteTenant(group.getId());
            } catch (RuntimeException e) {
                LOG.warn(String.format("Error occurred during finding of a site tenant for group %s ", group), e);
            }
        }
        return siteTenant;
    }

    /**
     * @return the tenant name, the display name of the group when no tenant is defined on its parent line,
     * or "N/A" when there is no group
     */
    @Transactional(readOnly = true)
    public String findSiteTenantNameFor(Group siteGroup) {
        String siteTenantName = NOTHING_FOUND;
        if (siteGroup != null) {
            SiteTenant siteTenant = findFor(siteGroup);
            if (siteTenant != null) {
                siteTenantName = siteTenant.getName();
            } else {
                siteTenantName = siteGroup.getDisplayName();
            }
        }
        return siteTenantName;
    }
}
