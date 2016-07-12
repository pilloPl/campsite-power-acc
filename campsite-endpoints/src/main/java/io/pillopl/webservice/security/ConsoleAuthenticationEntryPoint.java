package io.pillopl.webservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pillopl.webservice.console.common.FaultInfo;
import io.pillopl.webservice.console.common.WsErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers refused requests like a web service fault: 401 NOT_AUTHENTICATED without a login (no basic
 * authentication challenge, the console shows its own login), 403 NOT_AUTHORIZED for a missing or wrong CSRF token.
 */
@Component
public class ConsoleAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ConsoleAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, new FaultInfo(WsErrorCode.NOT_AUTHENTICATED, "Login required"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String message = accessDeniedException instanceof CsrfException ? "Missing or invalid CSRF token"
                : "Access denied";
        write(response, new FaultInfo(WsErrorCode.NOT_AUTHORIZED, message));
    }

    private void write(HttpServletResponse response, FaultInfo faultInfo) throws IOException {
        response.setStatus(faultInfo.errorCode().getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), faultInfo);
    }
}
