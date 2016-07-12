package io.pillopl.webservice.security;

import io.pillopl.api.UserApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.dto.user.CurrentUserDTO;
import io.pillopl.value.Username;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks username and password against USERINFO through the user service interface (the password hash never leaves
 * the services). The rights of the user become its authorities ({@code RIGHT_<name>}), its roles
 * {@code ROLE_<name>}; the logged-in user is the caller of every console call.
 */
@Component
public class ConsoleAuthenticationProvider implements AuthenticationProvider {

    static final String RIGHT_PREFIX = "RIGHT_";

    static final String ROLE_PREFIX = "ROLE_";

    private final UserApi userApi;

    public ConsoleAuthenticationProvider(UserApi userApi) {
        this.userApi = userApi;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        CurrentUserDTO user;
        try {
            user = userApi.authenticate(Username.of(username), credentials != null ? credentials.toString() : null);
        } catch (AccessNotAllowedException | InvalidInputException | IllegalArgumentException e) {
            throw new BadCredentialsException(e.getMessage());
        }
        UsernamePasswordAuthenticationToken result = UsernamePasswordAuthenticationToken.authenticated(
                user.username(), null, authorities(user));
        result.setDetails(user);
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    static List<GrantedAuthority> authorities(CurrentUserDTO user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String right : user.rights()) {
            authorities.add(new SimpleGrantedAuthority(RIGHT_PREFIX + right));
        }
        for (String role : user.roles()) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
        }
        return authorities;
    }
}
