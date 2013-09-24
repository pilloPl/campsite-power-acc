package io.pillopl.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardEventsTest {

    @Test
    void authorizationEventCarriesSocketCardAndAnswer() {
        Event event = new AuthorizationEvent(11L, null, "7001", "ACCEPTED");

        assertThat(event.getType()).isEqualTo(EventType.AUTHORIZATION);
        assertThat(EventAttribute.SOCKET_ID.getLongValue(event)).isEqualTo(11L);
        assertThat(event.getAttribute(EventAttribute.PEDESTAL_ID)).isNull();
        assertThat(AuthorizationEvent.getRfid(event)).isEqualTo("7001");
        assertThat(AuthorizationEvent.getAuthorizationResult(event)).isEqualTo("ACCEPTED");
    }

    @Test
    void adoptionCheckEventCarriesPedestalAndCard() {
        Event event = new CheckWelcomeCardAdoptionEvent("HR-BAY-P001", "7002");

        assertThat(event.getType()).isEqualTo(EventType.CHECK_WELCOME_CARD_ADOPTION);
        assertThat(CheckWelcomeCardAdoptionEvent.getPedestalCode(event)).isEqualTo("HR-BAY-P001");
        assertThat(CheckWelcomeCardAdoptionEvent.getRfid(event)).isEqualTo("7002");
    }

    @Test
    void houseCardChangedEventCarriesTheHouseCard() {
        Event event = new HouseCardChangedEvent(42L);

        assertThat(event.getType()).isEqualTo(EventType.HOUSE_CARD_CHANGED);
        assertThat(HouseCardChangedEvent.getHouseCardId(event)).isEqualTo(42L);
    }
}
