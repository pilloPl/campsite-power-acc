package io.pillopl.webservice.security;

import io.pillopl.api.AdministrationApi;
import io.pillopl.webservice.console.common.WebServicePaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Audits every state-changing console call (FRONTEND.md: every action that changes state is audited): a POST to
 * {@code /api/console/<service>/<operation>} whose operation is not a read ({@link #READ_OPERATION_PREFIXES}). The row
 * holds the logged-in user, {@code <service>/<operation>}, the request body and, for a refused or failed call, the
 * HTTP status with the fault message. Login attempts are audited by the user service itself (without the password);
 * the login path is skipped here.
 */
@Component
public class ConsoleActionAuditFilter extends OncePerRequestFilter {

    static final List<String> READ_OPERATION_PREFIXES = List.of("find", "get", "exists", "simulate", "preview",
            "list", "search", "count");

    private static final int MAX_BODY = 2000;

    private static final Pattern PASSWORD_FIELD = Pattern.compile("\"(\\w*[Pp]assword)\"\\s*:\\s*\"(?:[^\"\\\\]|\\\\.)*\"");

    private final AdministrationApi administrationApi;

    public ConsoleActionAuditFilter(AdministrationApi administrationApi) {
        this.administrationApi = administrationApi;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isAuditedAction(request.getMethod(), pathOf(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, MAX_BODY);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            audit(cachedRequest, cachedResponse);
            cachedResponse.copyBodyToResponse();
        }
    }

    static boolean isAuditedAction(String method, String path) {
        if (!"POST".equalsIgnoreCase(method) || path == null || !path.startsWith(WebServicePaths.CONSOLE + "/")) {
            return false;
        }
        if (path.startsWith(WebServicePaths.AUTH + "/login")) {
            return false;
        }
        String operation = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        for (String prefix : READ_OPERATION_PREFIXES) {
            if (operation.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private void audit(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        String operation = pathOf(request).substring(WebServicePaths.CONSOLE.length() + 1);
        int status = response.getStatus();
        String errorReason = null;
        if (status >= 400) {
            errorReason = "HTTP " + status + ": " + text(response.getContentAsByteArray());
        }
        administrationApi.auditConsoleAction(caller(), operation, maskPasswords(text(request.getContentAsByteArray())),
                errorReason);
    }

    /**
     * Passwords in a request (e.g. setPassword) never reach the audit table.
     */
    static String maskPasswords(String body) {
        return body == null ? null : PASSWORD_FIELD.matcher(body).replaceAll("\"$1\":\"***\"");
    }

    private static String caller() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    private static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length()) : uri;
    }

    private static String text(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String value = new String(bytes, 0, Math.min(bytes.length, MAX_BODY), StandardCharsets.UTF_8);
        return value.isBlank() ? null : value;
    }
}
