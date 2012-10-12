package io.pillopl.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests: full Spring context of the module under test on a Testcontainers PostgreSQL
 * with all Flyway migrations applied and Hibernate validating the mapping against the schema.
 * <p>
 * The module must provide its own {@code @SpringBootConfiguration} (the services module has a test-only
 * application in {@code io.pillopl}, the web module uses the real one). Shared with other modules through the
 * services test-jar. The container is started once per Spring test context (contexts are cached).
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.main.banner-mode=off"
})
@Import(PostgresTestcontainersConfiguration.class)
@ActiveProfiles("integration-test")
public abstract class IntegrationTest {
}
