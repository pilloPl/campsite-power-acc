package io.pillopl.webservice.console.auth;

import io.pillopl.api.UserApi;
import io.pillopl.dto.user.CurrentUserDTO;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login of the console: a successful login stores the user in the HTTP session (session cookie) under a new session
 * id; {@code currentUser} is a GET, so that the console can call it first and receive the XSRF-TOKEN cookie it needs
 * for the login and all other calls.
 */
@RestController
@RequestMapping(WebServicePaths.AUTH)
public class AuthWebService extends AbstractWebService<UserApi, AuthWebServiceFault> {

    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepository;

    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public AuthWebService(UserApi userApi, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        super(userApi);
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) throws AuthWebServiceFault {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                    request.username(), request.password()));
        } catch (AuthenticationException e) {
            throw createFault(e.getMessage(), WsErrorCode.NOT_AUTHENTICATED);
        }
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return new LoginResponse((CurrentUserDTO) authentication.getDetails());
    }

    @PostMapping("/logout")
    public LogoutResponse logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
        logoutHandler.logout(httpRequest, httpResponse, authentication);
        return new LogoutResponse(loggedIn);
    }

    @GetMapping("/currentUser")
    public GetCurrentUserResponse currentUser() throws AuthWebServiceFault {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw createFault("Login required", WsErrorCode.NOT_AUTHENTICATED);
        }
        return invoke("currentUser", () -> new GetCurrentUserResponse(getBean().getCurrentUser(caller())));
    }

    @Override
    protected AuthWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new AuthWebServiceFault(message, errorCode);
    }
}
