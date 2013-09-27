package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.WelcomeCardAdoptionDao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WelcomeCardAdoptionServiceTest {

    private final WelcomeCardAdoptionDao dao = mock(WelcomeCardAdoptionDao.class);
    private final WelcomeCardAdoptionService service = new WelcomeCardAdoptionService(dao,
            mock(io.pillopl.persistence.dao.PedestalDao.class), mock(io.pillopl.persistence.dao.CampingCardDao.class),
            mock(io.pillopl.group.GroupService.class), mock(io.pillopl.group.AffiliateGroupService.class),
            mock(io.pillopl.card.HouseCardService.class));

    private final Group root = group(1, null);
    private final Group campsite = group(2, root);
    private final Group section = group(3, campsite);

    private static Group group(long id, Group parent) {
        Group group = new Group(GroupType.SITE, "G" + id);
        group.setId(id);
        group.setParent(parent);
        return group;
    }

    @Test
    void nearestAttributeDecidesEvenWhenAnAncestorAllows() {
        when(dao.findForGroup(1L)).thenReturn(new WelcomeCardAdoption(true));
        when(dao.findForGroup(2L)).thenReturn(new WelcomeCardAdoption(false));

        assertThat(service.adoptionIsAllowedFor(section)).isFalse();
    }

    @Test
    void attributeIsInheritedFromAnAncestor() {
        when(dao.findForGroup(1L)).thenReturn(new WelcomeCardAdoption(true));

        assertThat(service.adoptionIsAllowedFor(section)).isTrue();
    }

    @Test
    void noAttributeOnTheLineMeansNoAdoption() {
        assertThat(service.adoptionIsAllowedFor(section)).isFalse();
        assertThat(service.adoptionIsAllowedFor(null)).isFalse();
    }
}
