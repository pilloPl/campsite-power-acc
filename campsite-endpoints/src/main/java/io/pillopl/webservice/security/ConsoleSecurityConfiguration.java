package io.pillopl.webservice.security;

import io.pillopl.webservice.console.common.WebServicePaths;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security of the backend.
 * <ul>
 *     <li>Console ({@code /api/console/**}): session cookie after the login of {@code AuthWebService}; state-changing
 *     calls carry the CSRF token of the {@code XSRF-TOKEN} cookie in the {@code X-XSRF-TOKEN} header (the cookie is
 *     set on every answer, so the console reads it after its first call).</li>
 *     <li>Payment app ({@code /api/qrpay/**}): HTTP basic with a console user having QRPAY_APP, no CSRF.</li>
 *     <li>Pedestal link ({@code /api/pedestal-link/**}): open, the pedestals are identified by their code.</li>
 *     <li>API description ({@code /v3/api-docs}, {@code /swagger-ui}): open.</li>
 * </ul>
 * The rights themselves are checked by the service interfaces with the logged-in user as caller.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ConsoleCorsProperties.class)
public class ConsoleSecurityConfiguration {

    static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ConsoleAuthenticationProvider provider,
                                                   SecurityContextRepository securityContextRepository,
                                                   ConsoleAuthenticationEntryPoint entryPoint) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setHeaderName(CSRF_HEADER);
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax"));
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(WebServicePaths.PEDESTAL_LINK + "/**", WebServicePaths.QR_PAY + "/**"))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authenticationProvider(provider)
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(entryPoint))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(WebServicePaths.AUTH + "/**").permitAll()
                        .requestMatchers(WebServicePaths.PEDESTAL_LINK + "/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll());
        return http.build();
    }

    /**
     * The login of the console stores the authentication in the HTTP session.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager consoleAuthenticationManager(ConsoleAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    /**
     * The Vite dev server of the console runs on another origin; it sends the session cookie (credentials).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(ConsoleCorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", CSRF_HEADER, "Authorization", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/v3/api-docs/**", configuration);
        return source;
    }
}
