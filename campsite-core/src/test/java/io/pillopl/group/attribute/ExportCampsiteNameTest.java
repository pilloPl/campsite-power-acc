package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportCampsiteNameTest {

    @Test
    void exportNameFallsBackToTheGroupDisplayNameThenToTheDefault() {
        Group group = new Group(GroupType.SITE, "BAY CAMPSITE", "Bay Campsite");

        assertThat(ExportCampsiteName.computeName(new ExportCampsiteName("Bay"), group, "x")).isEqualTo("Bay");
        assertThat(ExportCampsiteName.computeName(null, group, "x")).isEqualTo("Bay Campsite");
        assertThat(ExportCampsiteName.computeName(null, null, "x")).isEqualTo("x");
    }
}
