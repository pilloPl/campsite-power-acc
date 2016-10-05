package io.pillopl.power;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.commons.DateRange;
import io.pillopl.dto.power.PowerSectionDTO;
import io.pillopl.dto.power.SectionLoadDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.PowerSectionDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoStatus;
import io.pillopl.user.UserroleName;
import io.pillopl.value.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The power budget tab on the database: the sections in the scope of the user with their sockets, the load per hour
 * (hand-written SQL over the sessions and the measured currents) and the configuration changes.
 */
@Transactional
class PowerBudgetIntegrationTest extends IntegrationTest {

    static final Username TECHNICIAN = Username.of("power-technician");
    static final LocalDateTime DAY = LocalDateTime.of(2026, 7, 10, 0, 0);

    @Autowired PowerSectionService powerSectionService;
    @Autowired PowerSectionDao powerSectionDao;
    @Autowired SocketDao socketDao;
    @Autowired UserinfoDao userinfoDao;
    @Autowired UserroleDao userroleDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired JdbcTemplate jdbcTemplate;

    PowerSection dunes;
    Socket socket1;
    Socket socket2;

    @BeforeEach
    void setUp() {
        Group dunesSite = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "DUNES POWER",
                "Dunes");
        infrastructure.pedestal("HR-DUNES-P101", dunesSite, 3);
        dunes = powerSectionDao.saveAndFlush(new PowerSection("Dunes A", new BigDecimal("32"),
                NominalCurrentSumAlgorithm.BEAN_NAME));
        socket1 = socketDao.findByPedestalCodeAndSocketNo("HR-DUNES-P101", 1);
        socket2 = socketDao.findByPedestalCodeAndSocketNo("HR-DUNES-P101", 2);
        for (Socket socket : List.of(socket1, socket2)) {
            socket.setPowerSection(dunes);
            socketDao.saveAndFlush(socket);
        }
        user(TECHNICIAN.value(), UserroleName.SERVICE_TECHNICIAN, "HR SITES");
        user("power-desk", UserroleName.RECEPTION, "HR SITES");
        user("power-it", UserroleName.SERVICE_TECHNICIAN, "IT SITES");
    }

    @Test
    void sectionsOfTheScopeAreListedWithTheirSocketsAndAlgorithms() {
        List<PowerSectionDTO> sections = powerSectionService.findPowerSections(TECHNICIAN);

        assertThat(sections).filteredOn(s -> s.sectionName().equals("Dunes A")).singleElement().satisfies(s -> {
            assertThat(s.socketCount()).isEqualTo(2);
            assertThat(s.algorithmAvailable()).isTrue();
            assertThat(s.allocatedA()).isZero();
            assertThat(s.sockets()).extracting(PowerSectionDTO.SocketAllocation::pedestalCode,
                    PowerSectionDTO.SocketAllocation::socketNo).containsExactly(tuple("HR-DUNES-P101", 1),
                    tuple("HR-DUNES-P101", 2));
        });
        assertThat(powerSectionService.findPowerSections(Username.of("power-it")))
                .extracting(PowerSectionDTO::sectionName).doesNotContain("Dunes A");
        assertThat(powerSectionService.findAllocationAlgorithms(TECHNICIAN))
                .contains(NominalCurrentSumAlgorithm.BEAN_NAME).doesNotContain("powerSectionService");
    }

    @Test
    void loadPerHourCountsTheRatedCurrentOfTheRunningSessionsAndTheHighestMeasuredCurrents() {
        session(socket1, 950_001, DAY.plusHours(10).plusMinutes(30), DAY.plusHours(12).plusMinutes(15));
        session(socket2, 950_002, DAY.plusHours(11), DAY.plusHours(11).plusMinutes(40));
        reading(socket1, DAY.plusHours(10).plusMinutes(40), "12.5");
        reading(socket1, DAY.plusHours(10).plusMinutes(50), "14");
        reading(socket2, DAY.plusHours(11).plusMinutes(10), "9");

        SectionLoadDTO load = powerSectionService.getSectionLoad(TECHNICIAN, dunes.getId(),
                new DateRange(DAY.plusHours(10), DAY.plusHours(13)));

        assertThat(load.maxCurrentA()).isEqualByComparingTo("32");
        assertThat(load.points()).extracting(SectionLoadDTO.Point::hour, SectionLoadDTO.Point::allocatedA,
                SectionLoadDTO.Point::sessions).containsExactly(tuple(DAY.plusHours(10), 16, 1),
                tuple(DAY.plusHours(11), 32, 2), tuple(DAY.plusHours(12), 16, 1));
        assertThat(load.points()).extracting(p -> p.measuredA().stripTrailingZeros().toPlainString())
                .containsExactly("14", "9", "0");
    }

    @Test
    void loadIsRefusedForLongPeriodsAndOutsideTheScope() {
        assertThatThrownBy(() -> powerSectionService.getSectionLoad(TECHNICIAN, dunes.getId(),
                new DateRange(DAY, DAY.plusDays(40)))).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> powerSectionService.getSectionLoad(Username.of("power-it"), dunes.getId(), null))
                .isInstanceOf(ObjectNotFoundException.class);
        assertThat(powerSectionService.getSectionLoad(TECHNICIAN, dunes.getId(), null).points()).hasSizeBetween(24, 25);
    }

    @Test
    void sectionsAreCreatedWithAValidAlgorithmAndUniqueNames() {
        PowerSectionDTO created = powerSectionService.savePowerSection(TECHNICIAN, null, " Dunes B ",
                new BigDecimal("63"), NominalCurrentSumAlgorithm.BEAN_NAME);

        assertThat(created.sectionName()).isEqualTo("Dunes B");
        assertThat(created.maxCurrentA()).isEqualByComparingTo("63");
        assertThatThrownBy(() -> powerSectionService.savePowerSection(TECHNICIAN, null, "Dunes A",
                new BigDecimal("16"), NominalCurrentSumAlgorithm.BEAN_NAME)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> powerSectionService.savePowerSection(TECHNICIAN, null, "Dunes C",
                new BigDecimal("16"), "forecastAlgorithm")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> powerSectionService.savePowerSection(Username.of("power-desk"), null, "Dunes D",
                new BigDecimal("16"), NominalCurrentSumAlgorithm.BEAN_NAME))
                .isInstanceOf(AccessNotAllowedException.class);
    }

    @Test
    void capacityAlgorithmAndSocketsOfASectionAreChanged() {
        assertThat(powerSectionService.changeSectionCapacity(TECHNICIAN, dunes.getId(), new BigDecimal("16"))
                .maxCurrentA()).isEqualByComparingTo("16");
        assertThatThrownBy(() -> powerSectionService.changeSectionCapacity(TECHNICIAN, dunes.getId(),
                BigDecimal.ZERO)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> powerSectionService.selectAllocationAlgorithm(TECHNICIAN, dunes.getId(), " "))
                .isInstanceOf(InvalidInputException.class);
        assertThat(powerSectionService.selectAllocationAlgorithm(TECHNICIAN, dunes.getId(),
                NominalCurrentSumAlgorithm.BEAN_NAME).algorithmBeanName()).isEqualTo("nominalCurrentSum");

        Socket socket3 = socketDao.findByPedestalCodeAndSocketNo("HR-DUNES-P101", 3);
        assertThat(powerSectionService.assignSocketToSection(TECHNICIAN, socket3.getId(), dunes.getId())
                .socketCount()).isEqualTo(3);
        assertThat(powerSectionService.assignSocketToSection(TECHNICIAN, socket3.getId(), null)).isNull();
        assertThat(socketDao.findById(socket3.getId()).orElseThrow().getPowerSection()).isNull();
        assertThatThrownBy(() -> powerSectionService.assignSocketToSection(Username.of("power-it"), socket3.getId(),
                null)).isInstanceOf(ObjectNotFoundException.class);
    }

    private void session(Socket socket, int transactionId, LocalDateTime start, LocalDateTime stop) {
        jdbcTemplate.update("INSERT INTO SUPPLY_SESSION (SOCKET_ID, TRANSACTION_ID, CREATE_DATE, ACTIVE, MAX_CURRENT_A,"
                + " STOP_REASON, STOP_DATE) VALUES (?, ?, ?, FALSE, 16, 'LOCAL', ?)", socket.getId(), transactionId,
                start, stop);
    }

    private void reading(Socket socket, LocalDateTime at, String currentA) {
        jdbcTemplate.update("INSERT INTO SOCKET_STATUS_LOG (SOCKET_ID, CREATE_DATE, UPDATE_DATE, STATUS, CURRENT_A)"
                + " VALUES (?, ?, ?, 'SUPPLYING', ?)", socket.getId(), at, at, new BigDecimal(currentA));
    }

    private void user(String username, UserroleName role, String siteScope) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setStatus(UserinfoStatus.ACTIVE);
        user.setGroup(groupService.fetchStaffGroup("HR STAFF"));
        user.getGroups().add(groupService.fetchSiteGroup(siteScope));
        user.getRoles().add(userroleDao.findByName(role.name()));
        userinfoDao.saveAndFlush(user);
    }
}
