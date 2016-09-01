package io.pillopl.webservice.console.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI description of the backend ({@code /v3/api-docs}), the source of the typed client of the console. Console
 * calls run with the session cookie of the login; state-changing calls send the CSRF token as header.
 */
@Configuration
public class ConsoleOpenApiConfiguration {

    static final String SESSION_COOKIE = "sessionCookie";

    static final String CSRF_HEADER = "csrfHeader";

    @Bean
    public OpenAPI campsitePowerOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Campsite Power backend")
                        .description("Console web services (POST " + WebServicePaths.CONSOLE
                                + "/<service>/<operation>, faults as FaultInfo with WsErrorCode), the payment app ("
                                + WebServicePaths.QR_PAY + ") and the pedestal link (" + WebServicePaths.PEDESTAL_LINK
                                + "/v1|v2|v3/<pedestal code>/<message>).")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes(SESSION_COOKIE, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                                .description("Session after POST " + WebServicePaths.AUTH + "/login"))
                        .addSecuritySchemes(CSRF_HEADER, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-XSRF-TOKEN")
                                .description("Value of the XSRF-TOKEN cookie, needed by every POST of the console")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_COOKIE).addList(CSRF_HEADER));
    }
}
