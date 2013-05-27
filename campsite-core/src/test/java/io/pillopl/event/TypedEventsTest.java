package io.pillopl.event;

import io.pillopl.user.UserinfoChangeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypedEventsTest {

    @Test
    void commissionedEventCarriesPedestalGroupAndInstaller() {
        Event event = new PedestalCommissionedEvent(7L, "HR-BAY-P001", 1003L, "installer");

        assertThat(event.getType()).isEqualTo(EventType.PEDESTAL_COMMISSIONED);
        assertThat(PedestalCommissionedEvent.getPedestalId(event)).isEqualTo(7L);
        assertThat(PedestalCommissionedEvent.getGroupId(event)).isEqualTo(1003L);
        assertThat(EventAttribute.USERNAME.getStringValue(event)).isEqualTo("installer");
    }

    @Test
    void decommissionedAndAccessModeEventsCarryTheirKeys() {
        Event back = new PedestalDecommissionedEvent(7L, "HR-BAY-P001", 1001L, "owner");
        Event switched = new AccessModeSwitchedEvent(1003L, "NETWORK_ACCESS");

        assertThat(PedestalDecommissionedEvent.getPedestalId(back)).isEqualTo(7L);
        assertThat(AccessModeSwitchedEvent.getGroupId(switched)).isEqualTo(1003L);
        assertThat(AccessModeSwitchedEvent.getAccessMode(switched)).isEqualTo("NETWORK_ACCESS");
    }

    @Test
    void userChangedEventGivesBackTheChangeType() {
        Event event = new UserinfoChangedEvent(5L, "owner", UserinfoChangeType.AFFILIATE_OWNER_SETUP);

        assertThat(UserinfoChangedEvent.getChangeType(event)).isEqualTo(UserinfoChangeType.AFFILIATE_OWNER_SETUP);
        assertThat(EventAttribute.ID.getLongValue(event)).isEqualTo(5L);
    }
}
