package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * Tree of the calling user's scope group of one tree.
 */
public record GetGroupTreeRequest(String authUsername, String groupType) implements Serializable {
}
