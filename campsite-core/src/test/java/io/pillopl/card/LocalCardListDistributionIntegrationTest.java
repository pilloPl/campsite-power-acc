package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.infrastructure.PedestalType;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalTypeDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class LocalCardListDistributionIntegrationTest extends IntegrationTest {

    @Autowired PedestalDao pedestalDao;
    @Autowired PedestalTypeDao pedestalTypeDao;
    @Autowired GroupService groupService;
    @Autowired InfrastructureFixture infrastructure;

    @Test
    void onlyLivingPedestalsWithAutomatedUpdatesAndAControllerListAreDistributedTo() {
        Group sites = groupService.fetchSiteGroup("HR SITES");
        Pedestal automated = automated(infrastructure.pedestal("HR-BAY-P001", sites, 1));
        infrastructure.pedestal("HR-BAY-P002", sites, 1);
        Pedestal deleted = automated(infrastructure.pedestal("HR-BAY-P003", sites, 1));
        deleted.setDeleted(true);
        Pedestal noList = infrastructure.pedestal("HR-BAY-P004", sites, 1);
        PedestalType simple = new PedestalType("ACME PEDESTALS", "P2 simple", PedestalLinkVersion.V1);
        noList.setPedestalType(pedestalTypeDao.saveAndFlush(simple));
        automated(noList);
        pedestalDao.flush();

        assertThat(pedestalDao.findPedestalsForLocalCardListDistribution()).containsExactly(automated);
    }

    private Pedestal automated(Pedestal pedestal) {
        pedestal.setAutomatedWhitelistUpdates(true);
        return pedestalDao.saveAndFlush(pedestal);
    }
}
