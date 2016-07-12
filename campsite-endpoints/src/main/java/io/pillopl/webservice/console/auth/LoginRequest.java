package io.pillopl.webservice.console.auth;

import java.io.Serializable;

/**
 * Console login with username and password (the console sends the CSRF token of the XSRF-TOKEN cookie as header).
 */
public record LoginRequest(String username, String password) implements Serializable {

    @Override
    public String toString() {
        return "LoginRequest{username='" + username + "'}";
    }
}
