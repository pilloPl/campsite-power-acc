package io.pillopl.webservice.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origins allowed to call the backend from a browser with credentials ({@code campsite.console.cors.allowed-origins},
 * by default the Vite dev server of the console).
 */
@ConfigurationProperties(prefix = "campsite.console.cors")
public record ConsoleCorsProperties(List<String> allowedOrigins) {

    public ConsoleCorsProperties {
        allowedOrigins = allowedOrigins != null && !allowedOrigins.isEmpty() ? List.copyOf(allowedOrigins)
                : List.of("http://localhost:5173", "http://127.0.0.1:5173");
    }
}
