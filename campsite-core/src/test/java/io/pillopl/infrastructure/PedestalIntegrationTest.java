package io.pillopl.infrastructure;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PedestalIntegrationTest extends IntegrationTest {

    @Autowired InfrastructureFixture fixture;
    @Autowired PedestalDao pedestalDao;
    @Autowired SocketDao socketDao;
    @Autowired GroupService groupService;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void pedestalIsStoredWithItsSocketsInPositionOrder() {
        Group hrSites = groupService.fetchSiteGroup("HR SITES");
        Long id = fixture.pedestal("HR-BAY-P001", hrSites, 3).getId();
        entityManager.clear();

        Pedestal reloaded = pedestalDao.findByCode("HR-BAY-P001");

        assertThat(reloaded.getId()).isEqualTo(id);
        assertThat(reloaded.getSockets()).extracting(Socket::getPosition).containsExactly(1, 2, 3);
        assertThat(reloaded.getSockets()).allMatch(s -> s.getStatus() == SocketStatus.UNKNOWN);
        assertThat(reloaded.isListedInApp()).isTrue();
        assertThat(reloaded.isBillable()).isFalse();
        assertThat(reloaded.findSocketBySocketNo(2).getName()).isEqualTo("HR-BAY-P001_2");
    }

    @Test
    void deletedSocketsAreNotPartOfThePedestal() {
        Pedestal pedestal = fixture.pedestal("HR-BAY-P002", groupService.fetchSiteGroup("HR SITES"), 2);
        jdbcTemplate.update("UPDATE SOCKET SET DELETED = TRUE WHERE PEDESTAL_ID = ? AND POS = 2", pedestal.getId());
        entityManager.clear();

        assertThat(pedestalDao.findByCode("HR-BAY-P002").getSockets()).hasSize(1);
        assertThat(socketDao.findActiveByPedestalId(pedestal.getId())).hasSize(1);
    }

    @Test
    void pedestalIsFoundOnlyInTheGroupsItHangsIn() {
        Group hrSites = groupService.fetchSiteGroup("HR SITES");
        Group itSites = groupService.fetchSiteGroup("IT SITES");
        Pedestal pedestal = fixture.pedestal("HR-BAY-P003", hrSites, 1);

        assertThat(pedestalDao.isInGroupList(pedestal.getId(), List.of(hrSites.getId()))).isEqualTo(pedestal);
        assertThat(pedestalDao.isInGroupList(pedestal.getId(), List.of(itSites.getId()))).isNull();
        assertThat(pedestalDao.findByGroupIds(List.of(hrSites.getId()))).containsExactly(pedestal);
    }

    @Test
    void socketIsFoundByPedestalCodeAndSocketNumber() {
        Pedestal pedestal = fixture.pedestal("HR-BAY-P004", groupService.fetchSiteGroup("HR SITES"), 2);

        Socket socket = socketDao.findByPedestalCodeAndSocketNo("HR-BAY-P004", 2);

        assertThat(socket.getPedestal()).isEqualTo(pedestal);
        assertThat(pedestalDao.findBySocketId(socket.getId())).isEqualTo(pedestal);
        assertThat(socket.getMaxCurrentInAmperes()).isEqualTo(16);
    }
}
