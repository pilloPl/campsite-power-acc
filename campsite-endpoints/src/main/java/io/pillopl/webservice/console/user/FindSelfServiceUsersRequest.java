package io.pillopl.webservice.console.user;

import java.io.Serializable;

/**
 * Self-service users in the calling user's STAFF scope; the flags filter when given.
 */
public record FindSelfServiceUsersRequest(Boolean affiliateOwner, Boolean seasonalResident,
                                          Integer page, Integer rowsPerPage)
        implements Serializable {
}
