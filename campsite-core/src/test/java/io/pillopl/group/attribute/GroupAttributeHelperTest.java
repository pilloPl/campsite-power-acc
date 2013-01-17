package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupAttributeHelperTest {

    private static SiteTenant tenantOn(long groupId, String name) {
        Group group = new Group(GroupType.SITE, "G" + groupId);
        group.setId(groupId);
        SiteTenant tenant = new SiteTenant(name);
        tenant.setGroup(group);
        return tenant;
    }

    @Test
    void attributeOfTheNearestGroupOnTheParentLineWins() {
        SiteTenant onRoot = tenantOn(1, "root");
        SiteTenant onMiddle = tenantOn(2, "middle");

        assertThat(GroupAttributeHelper.findNearest(List.of(3L, 2L, 1L), List.of(onRoot, onMiddle))).isSameAs(onMiddle);
    }

    @Test
    void attributesOutsideTheParentLineAreIgnored() {
        SiteTenant elsewhere = tenantOn(7, "elsewhere");

        assertThat(GroupAttributeHelper.findNearest(List.of(3L, 2L, 1L), List.of(elsewhere))).isNull();
    }

    @Test
    void attributeThatDoesNotApplyToTheQuestionLetsTheNextOneDecide() {
        SiteTenant onMiddle = tenantOn(2, "middle");
        SiteTenant onRoot = tenantOn(1, "root");

        SiteTenant found = GroupAttributeHelper.findNearest(List.of(2L, 1L), List.of(onMiddle, onRoot),
                tenant -> tenant.getName().startsWith("r"));

        assertThat(found).isSameAs(onRoot);
    }

    @Test
    void missingInputMeansNoAttribute() {
        assertThat(GroupAttributeHelper.<SiteTenant>findNearest(null, List.of())).isNull();
        assertThat(GroupAttributeHelper.<SiteTenant>findNearest(List.of(1L), null)).isNull();
    }

    @Test
    void attributesOfLaterIslandsAreListedWithTheirTask() {
        assertThat(GroupAttributeType.HOUSE_CARD.isImplemented()).isFalse();
        assertThat(GroupAttributeType.HOUSE_CARD.getOwningModule()).isEqualTo("cards");
        assertThat(GroupAttributeType.DEFAULT_TARIFF.getOwningModule()).isEqualTo("pricing");
        assertThat(GroupAttributeType.SITE_TENANT.isImplemented()).isTrue();
    }
}
