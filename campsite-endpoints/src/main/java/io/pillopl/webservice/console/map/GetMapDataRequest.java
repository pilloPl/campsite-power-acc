package io.pillopl.webservice.console.map;

import java.io.Serializable;

/**
 * The map of the caller's campsites, optionally of one campsite group.
 */
public record GetMapDataRequest(Long campsiteGroupId) implements Serializable {
}
