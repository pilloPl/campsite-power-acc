package io.pillopl.webservice.console.user;

import java.io.Serializable;
import java.util.List;

/**
 * The direct roles a user has from now on (roles of its security groups stay).
 */
public record ReplaceUserRolesRequest(String username, List<String> roles) implements Serializable {
}
