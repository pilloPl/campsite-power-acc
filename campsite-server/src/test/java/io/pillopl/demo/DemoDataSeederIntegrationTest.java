package io.pillopl.demo;

import io.pillopl.api.ConsumptionRecordApi;
import io.pillopl.api.GroupApi;
import io.pillopl.api.PedestalApi;
import io.pillopl.api.ReportApi;
import io.pillopl.api.TariffApi;
import io.pillopl.api.UserApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.user.CurrentUserDTO;
import io.pillopl.test.IntegrationTest;
import io.pillopl.value.Username;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The demo seed (profile demo): seeded at start-up and once more by the test, so it must be repeatable; every console
 * tab finds data, the demo users log in with their documented passwords.
 */
@ActiveProfiles({"integration-test", "demo"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoDataSeederIntegrationTest extends IntegrationTest {

    static final Username ADMIN = Username.of("admin");

    @Autowired DemoDataSeeder seeder;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UserApi userApi;
    @Autowired ConsumptionRecordApi consumptionRecordApi;
    @Autowired ReportApi reportApi;
    @Autowired TariffApi tariffApi;
    @Autowired PedestalApi pedestalApi;
    @Autowired GroupApi groupApi;
    @Autowired io.pillopl.api.DashboardApi dashboardApi;
    @Autowired io.pillopl.api.MapApi mapApi;
    @Autowired io.pillopl.api.SupplyApi supplyApi;
    @Autowired io.pillopl.api.MonitoringApi monitoringApi;
    @Autowired io.pillopl.api.QrPayConsoleApi qrPayConsoleApi;
    @Autowired io.pillopl.api.PowerBudgetApi powerBudgetApi;
    @Autowired io.pillopl.api.ErpBillingApi erpBillingApi;
    @Autowired io.pillopl.api.IssuerApi issuerApi;
    @Autowired io.pillopl.api.GuestApi guestApi;
    @Autowired io.pillopl.api.JobApi jobApi;

    long sessionsOfFirstSeed;

    @BeforeAll
    void seedAgain() {
        assertThat(seeder.isSeeded()).isTrue();
        sessionsOfFirstSeed = count("SELECT COUNT(*) FROM SUPPLY_SESSION WHERE ID >= 900000000");
        seeder.seed();
    }

    @Test
    void seedingAgainReplacesTheDemoDataInsteadOfAddingToIt() {
        assertThat(count("SELECT COUNT(*) FROM SUPPLY_SESSION WHERE ID >= 900000000")).isEqualTo(sessionsOfFirstSeed);
        assertThat(count("SELECT COUNT(*) FROM USERINFO WHERE USERNAME = 'admin'")).isEqualTo(1);
    }

    @Test
    void demoUsersLogInWithTheirDocumentedPasswordsAndHaveTheRightsOfTheirRole() {
        for (String username : DemoDataSeeder.USERS) {
            assertThat(userApi.authenticate(Username.of(username), "demo-" + username).username()).isEqualTo(username);
        }
        assertThat(rights("reception")).contains("GUEST_EDIT", "STAY_CHECKOUT", "CARD_EDIT").doesNotContain("CONFIG_EDIT");
        assertThat(rights("host")).contains("TARIFF_EDIT", "SOCKET_REMOTE_CONTROL").doesNotContain("USER_ADMIN");
        assertThat(rights("service")).contains("PEDESTAL_COMMISSION", "SOCKET_REMOTE_CONTROL");
        assertThat(rights("backoffice")).contains("BILLING_EDIT", "CONSUMPTION_RECORD_REVIEW");
        assertThat(rights("admin")).contains("USER_ADMIN", "CONFIG_EDIT", "AUDIT_VIEW", "BILLING_EDIT");
        assertThat(rights("qrpay-app")).containsExactly("QRPAY_APP");
        assertThatThrownBy(() -> userApi.authenticate(Username.of("trainee"), "demo-trainee"))
                .isInstanceOf(AccessNotAllowedException.class);
    }

    @Test
    void everyCampsiteHasItsMapPedestalsWithCoordinatesAndSockets() {
        List<Map<String, Object>> campsites = jdbcTemplate.queryForList("""
                SELECT g.ID,
                       (SELECT COUNT(*) FROM CAMPSITE_MAP_AREA a WHERE a.SITE_GROUP_ID = g.ID AND a.AREA_TYPE = 'PITCH') AS PITCHES,
                       (SELECT COUNT(*) FROM CAMPSITE_MAP_AREA a WHERE a.SITE_GROUP_ID = g.ID AND a.AREA_TYPE = 'SECTION'
                           AND a.POWER_SECTION_ID IS NOT NULL) AS SECTIONS,
                       (SELECT COUNT(*) FROM PEDESTAL p JOIN LOCATION l ON l.ID = p.LOCATION_ID
                         WHERE p.GROUP_ID = g.ID AND l.LATITUDE IS NOT NULL) AS PEDESTALS,
                       (SELECT COUNT(DISTINCT pt.LINK_VERSION) FROM PEDESTAL p JOIN PEDESTAL_TYPE pt ON pt.ID = p.PEDESTAL_TYPE_ID
                         WHERE p.GROUP_ID = g.ID) AS LINK_VERSIONS,
                       (SELECT COUNT(*) FROM SOCKET s JOIN PEDESTAL p ON p.ID = s.PEDESTAL_ID WHERE p.GROUP_ID = g.ID) AS SOCKETS
                  FROM TBL_GROUP g
                 WHERE g.ID BETWEEN 900000001 AND 900000003
                """);
        assertThat(campsites).hasSize(3).allSatisfy(c -> {
            assertThat(((Number) c.get("PITCHES")).intValue()).isEqualTo(24);
            assertThat(((Number) c.get("SECTIONS")).intValue()).isEqualTo(2);
            assertThat(((Number) c.get("PEDESTALS")).intValue()).isEqualTo(6);
            assertThat(((Number) c.get("LINK_VERSIONS")).intValue()).isEqualTo(3);
            assertThat(((Number) c.get("SOCKETS")).intValue()).isEqualTo(24);
        });
        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT STATUS FROM SOCKET WHERE ID >= 900000000", String.class))
                .contains("SUPPLYING", "AVAILABLE", "FAULTED", "BREAKER_TRIPPED");
        assertThat(count("SELECT COUNT(*) FROM PEDESTAL_STATE WHERE PEDESTAL_ID >= 900000000 AND TIMEOUT")).isEqualTo(1);
    }

    @Test
    void aMonthOfSuppliesWithRecordsOfEveryChannelAndTheReviewQueue() {
        assertThat(count("SELECT EXTRACT(DAY FROM MAX(CREATE_DATE) - MIN(CREATE_DATE)) FROM SUPPLY_SESSION "
                + "WHERE ID >= 900000000")).isGreaterThanOrEqualTo(40);
        assertThat(count("SELECT COUNT(*) FROM SUPPLY_SESSION WHERE ID >= 900000000 AND ACTIVE")).isGreaterThan(10);
        assertThat(count("SELECT COUNT(*) FROM SUPPLY_SESSION WHERE ID >= 900000000 AND ACTIVE "
                + "AND CREATE_DATE < now() - INTERVAL '30 hours' AND SOCKET_ID IN "
                + "(SELECT ID FROM SOCKET WHERE PEDESTAL_ID = 900000306)")).isGreaterThan(0);
        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT PAYMENT_CHANNEL FROM CONSUMPTION_RECORD "
                + "WHERE ID >= 900000000", String.class)).containsExactlyInAnyOrder("CARD", "HOUSE_CARD", "QR_APP");
        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT EXPORT_STATUS FROM CONSUMPTION_RECORD "
                + "WHERE ID >= 900000000", String.class))
                .contains("NEW", "TRANSFERRED", "PROCESSED", "ERROR", "REVIEW", "IGNORED");
        assertThat(count("SELECT COUNT(*) FROM CONSUMPTION_RECORD_SUSPECT WHERE ID >= 900000000 "
                + "AND VALIDATION_STATE = 'SUSPECT'")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM CONSUMPTION_RECORD_SUSPECT WHERE ID >= 900000000 "
                + "AND PRICING_STATUS = 'PRICING_FAILED'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM SOCKET_UTILIZATION WHERE SOCKET_ID >= 900000000")).isGreaterThan(10000);
        assertThat(count("SELECT COUNT(*) FROM STAY WHERE ID >= 900000000 AND CHECK_OUT_DATE IS NULL")).isGreaterThan(20);
        assertThat(count("SELECT COUNT(*) FROM STAY WHERE ID >= 900000000 AND CHECK_OUT_DATE IS NOT NULL"))
                .isGreaterThan(100);
        assertThat(count("SELECT COUNT(*) FROM SEASONAL_CONTRACT WHERE ID >= 900000000")).isEqualTo(2);
    }

    @Test
    void billingQrPaymentsPartnerEventsAndAuditHaveTheirRows() {
        assertThat(count("SELECT COUNT(*) FROM ERP_BILLING_LINE WHERE ID >= 900000000 AND EXPORT_STATUS = 'ERROR' "
                + "AND ERROR_TEXT IS NOT NULL")).isGreaterThan(0);
        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT LINE_TYPE FROM ERP_BILLING_LINE WHERE ID >= 900000000",
                String.class)).containsExactlyInAnyOrder("POWER", "FIXED_FEE", "PACKAGE_OVERAGE");
        assertThat(jdbcTemplate.queryForList("SELECT STATE FROM QRPAY_PROCESS WHERE ID >= 900000000 "
                + "AND NEXT_PROCESS_ID IS NULL", String.class))
                .contains("CLOSED", "START_CONFIRMED", "SOCKET_START_WAITING", "ERROR");
        assertThat(count("SELECT COUNT(*) FROM SUPPLY_SESSION WHERE ID >= 900000000 AND QRPAY_PROCESS_ID IS NOT NULL"))
                .isGreaterThan(5);
        assertThat(jdbcTemplate.queryForList("SELECT STATUS FROM ISSUER_SYNC_STATUS WHERE ISSUER_PARTNER_ID = 900000001",
                String.class)).containsExactlyInAnyOrder("OK", "ERROR");
        assertThat(count("SELECT COUNT(*) FROM EVENT_RETRY WHERE ID >= 900000000 AND STATUS = 'FAILED'")).isEqualTo(2);
        assertThat(count("SELECT COUNT(DISTINCT ACTION_TYPE) FROM AUDIT WHERE ID >= 900000000")).isGreaterThan(3);
        assertThat(count("SELECT COUNT(*) FROM HOUSECARD WHERE ID >= 900000000")).isEqualTo(15);
    }

    @Test
    void consoleReadsOfTheAdministratorWorkOnTheSeed() {
        LocalDate today = LocalDate.now();
        assertThat(consumptionRecordApi.findConsumptionRecords(ADMIN,
                DateRange.ofDays(today.minusDays(30).toString(), today.toString()), null,
                Pagination.ofNullable(0, 50))).hasSize(50);
        assertThat(consumptionRecordApi.findConsumptionRecords(ADMIN,
                DateRange.ofDays(today.minusDays(30).toString(), today.toString()), "SUSPECT", null)).isNotEmpty();
        assertThat(reportApi.getReport(ADMIN, "MONTHLY_CONSUMPTION", null, 900000001L, null, today.getYear(), null,
                null, null)).isNotEmpty();
        assertThat(tariffApi.findTariffs(ADMIN)).extracting("name").contains("HR NIGHT SAVER");
        assertThat(pedestalApi.findPedestalsByState(ADMIN, null, Pagination.ofNullable(0, 100)).items())
                .extracting("code").contains("ISTRIABAY-P01", "ISTRIABAY-P06");
        assertThat(groupApi.getGroupTree(ADMIN, "SITE").toString()).contains("ISTRIA BAY CAMP");
    }

    private List<String> rights(String username) {
        CurrentUserDTO user = userApi.getCurrentUser(Username.of(username));
        return user.rights();
    }

    @Test
    void theNewConsoleTabsFindDataOnTheSeed() {
        LocalDate today = LocalDate.now();
        DateRange month = DateRange.ofDays(today.minusDays(30).toString(), today.toString());
        var dashboard = dashboardApi.getDashboard(ADMIN, null);
        assertThat(dashboard.activeSessions()).isPositive();
        assertThat(dashboard.reviewQueue()).isPositive();
        assertThat(dashboardApi.findAlerts(ADMIN, null, false, 100)).isNotEmpty();
        assertThat(mapApi.getMapData(ADMIN, null).campsites()).isNotEmpty();
        assertThat(supplyApi.findSupplySessions(ADMIN, null, null, null, false, month, Pagination.ofNullable(0, 20))
                .items()).hasSize(20);
        assertThat(supplyApi.findSupplyIncidents(ADMIN, null, null, false, month, null).items()).isNotEmpty();
        assertThat(monitoringApi.getStatusReport(ADMIN, today.withDayOfMonth(1), null)).isNotEmpty();
        assertThat(monitoringApi.findHeartbeatTimeouts(Username.of("admin.fr"), null)).isNotEmpty();
        assertThat(monitoringApi.findFailedEvents(ADMIN, 50)).isNotEmpty();
        assertThat(qrPayConsoleApi.findQrPayProcesses(ADMIN, null, null, null, null).items()).isNotEmpty();
        assertThat(powerBudgetApi.findPowerSections(ADMIN)).isNotEmpty();
        assertThat(erpBillingApi.findErpBillingRuns(ADMIN)).isNotEmpty();
        assertThat(issuerApi.findIssuerPartners(ADMIN)).isNotEmpty();
        assertThat(issuerApi.findIssuerSyncStatuses(ADMIN)).isNotEmpty();
        assertThat(guestApi.findGuests(ADMIN, "a")).isNotEmpty();
        assertThat(guestApi.findSeasonalContracts(ADMIN, null)).isNotEmpty();
        assertThat(reportApi.getReport(ADMIN, "SOCKET_UTILIZATION", month, null, null, null, null, null, null))
                .isNotEmpty();
        assertThat(jobApi.findJobs(ADMIN)).isNotEmpty();
    }

    private long count(String sql) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class);
        return value != null ? value.longValue() : 0;
    }
}
