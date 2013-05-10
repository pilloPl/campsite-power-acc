package io.pillopl.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * Default {@link DecommissioningNotifier}: logs instead of mailing. A mailing implementation marked
 * {@code @Primary} replaces it.
 */
@Configuration(proxyBeanMethods = false)
public class LoggingDecommissioningNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingDecommissioningNotifier.class);

    @Bean
    DecommissioningNotifier decommissioningNotifier() {
        return (locale, pedestalCode, email) ->
                LOG.info(String.format("Pedestal %s back to supplier, owner %s (%s) not mailed yet", pedestalCode,
                        email, locale));
    }
}
