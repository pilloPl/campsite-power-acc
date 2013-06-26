package io.pillopl.webservice.console.user;

import io.pillopl.dto.user.UserinfoSummaryDTO;

import java.io.Serializable;
import java.util.List;

/**
 * One page of the self-service users.
 */
public record FindSelfServiceUsersResponse(List<UserinfoSummaryDTO> users, long total, Integer page,
                                           Integer rowsPerPage)
        implements Serializable {

    public FindSelfServiceUsersResponse {
        users = users != null ? List.copyOf(users) : List.of();
    }
}
