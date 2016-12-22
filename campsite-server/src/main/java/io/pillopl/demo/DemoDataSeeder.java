package io.pillopl.demo;

import io.pillopl.config.SystemPropertyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Demo data of the console (profile {@code demo}, never in tests or production): three campsites with map, pedestals,
 * guests, stays and 44 days of supplies, see {@code db/demo/*.sql}. Runs at start-up in one transaction: removes an
 * earlier seed (ids from 900000000 on, with what the application created on top of it), inserts the data relative
 * to now, gives the demo users their passwords ({@code demo-<username>}) and refreshes the materialized views and
 * the configuration cache. With {@code campsite.demo.reseed=false} an existing seed is kept. It starts before the
 * Quartz scheduler (lower lifecycle phase), so that no job works on the rows while they are replaced.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements SmartLifecycle {

    /**
     * Before the Quartz scheduler, which starts in the last phase.
     */
    static final int PHASE = 0;

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    static final String SCRIPTS = "classpath:db/demo/*.sql";

    static final String PASSWORD_PREFIX = "demo-";

    /**
     * Demo users with a password; {@code trainee} is invited and has none yet.
     */
    public static final List<String> USERS = List.of("reception", "host", "service", "backoffice", "admin", "admin.it",
            "admin.fr", "qrpay-app");

    private final JdbcTemplate jdbcTemplate;

    private final PasswordEncoder passwordEncoder;

    private final SystemPropertyService systemPropertyService;

    private final TransactionTemplate transactionTemplate;

    private final boolean reseed;

    private volatile boolean running;

    public DemoDataSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                          SystemPropertyService systemPropertyService, PlatformTransactionManager transactionManager,
                          @Value("${campsite.demo.reseed:true}") boolean reseed) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.systemPropertyService = systemPropertyService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.reseed = reseed;
    }

    @Override
    public void start() {
        running = true;
        if (!reseed && isSeeded()) {
            LOG.info("Demo data present, campsite.demo.reseed=false: kept");
            return;
        }
        seed();
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    public boolean isSeeded() {
        Integer users = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM USERINFO WHERE ID >= 900000000",
                Integer.class);
        return users != null && users > 0;
    }

    /**
     * Removes the earlier demo data and seeds it again (repeatable).
     */
    public void seed() {
        long started = System.currentTimeMillis();
        transactionTemplate.executeWithoutResult(status -> {
            for (Resource script : scripts()) {
                LOG.info("Demo data: " + script.getFilename());
                jdbcTemplate.execute(read(script));
            }
            for (String username : USERS) {
                jdbcTemplate.update("UPDATE USERINFO SET PASSWORD_HASH = ?, FAILED_LOGINS = 0 WHERE USERNAME = ? "
                        + "AND ID >= 900000000", passwordEncoder.encode(PASSWORD_PREFIX + username), username);
            }
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW SOCKET_UTILIZATION");
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW USERINFO_SUMMARY");
        });
        systemPropertyService.reloadSystemProperties();
        LOG.info(String.format("Demo data seeded in %d ms: %d supply sessions, %d consumption records",
                System.currentTimeMillis() - started,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SUPPLY_SESSION WHERE ID >= 900000000", Long.class),
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CONSUMPTION_RECORD WHERE ID >= 900000000",
                        Long.class)));
    }

    static List<Resource> scripts() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(SCRIPTS);
            return Arrays.stream(resources).sorted(Comparator.comparing(Resource::getFilename)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Demo scripts not found", e);
        }
    }

    private static String read(Resource script) {
        try {
            return script.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Demo script not readable: " + script.getFilename(), e);
        }
    }
}
