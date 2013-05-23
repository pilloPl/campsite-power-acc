package io.pillopl.dto.group;

import java.io.Serializable;

/**
 * Effective (nearest ancestor wins) attribute values of a group as shown in the organisation tab.
 */
public record GroupAttributesDTO(Long groupId, String type, String market, String siteTenantName,
                                 String issuerTenantName, boolean localList, boolean communicationLog,
                                 boolean welcomeCardAdoption, String exportCampsiteName, boolean supplierStock,
                                 String accessMode) implements Serializable {
}
