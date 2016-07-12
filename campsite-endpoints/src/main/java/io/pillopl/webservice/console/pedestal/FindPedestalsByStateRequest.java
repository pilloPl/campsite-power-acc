package io.pillopl.webservice.console.pedestal;

import java.io.Serializable;

/**
 * Pedestal list of the calling user's SITE scope; without page and rows per page the whole list.
 */
public record FindPedestalsByStateRequest(String state, Integer page, Integer rowsPerPage)
        implements Serializable {
}
