package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.TenantAttributeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the card issuer (issuer tenant) of an ISSUER group: the nearest tenant on the parent line.
 */
@Service
public class IssuerTenantService {

    private static final Logger LOG = LoggerFactory.getLogger(IssuerTenantService.class);

    static final String NOTHING_FOUND = "N/A";

    private final TenantAttributeDao tenantAttributeDao;

    public IssuerTenantService(TenantAttributeDao tenantAttributeDao) {
        this.tenantAttributeDao = tenantAttributeDao;
    }

    /**
     * @return the nearest {@link IssuerTenant} or null when the group is null, not an ISSUER group or has none
     */
    @Transactional(readOnly = true)
    public IssuerTenant findFor(Group group) {
        IssuerTenant issuerTenant = null;
        if (group != null && GroupType.ISSUER.isGroupType(group)) {
            try {
                issuerTenant = tenantAttributeDao.findNearestIssuerTenant(group.getId());
            } catch (RuntimeException e) {
                LOG.warn(String.format("Error occurred during finding of an issuer tenant for group %s ", group), e);
            }
        }
        return issuerTenant;
    }

    /**
     * @return the tenant name, the name of the group when no tenant is defined on its parent line,
     * or "N/A" when there is no group
     */
    @Transactional(readOnly = true)
    public String findIssuerTenantNameFor(Group issuerGroup) {
        String issuerTenantName = NOTHING_FOUND;
        if (issuerGroup != null) {
            IssuerTenant issuerTenant = findFor(issuerGroup);
            if (issuerTenant != null) {
                issuerTenantName = issuerTenant.getName();
            } else {
                issuerTenantName = issuerGroup.getName();
            }
        }
        return issuerTenantName;
    }
}
