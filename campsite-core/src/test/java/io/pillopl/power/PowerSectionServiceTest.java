package io.pillopl.power;

import io.pillopl.event.Event;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.event.PowerSectionMisconfiguredEvent;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.supply.SupplySession;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PowerSectionServiceTest {

    static final SocketLinkId SOCKET_1 = SocketLinkId.of("HR-BAY-P101", 1);

    final PedestalService pedestalService = mock(PedestalService.class);
    final SupplySessionManager supplySessionManager = mock(SupplySessionManager.class);
    final EventService eventService = mock(EventService.class);
    final ApplicationContext applicationContext = mock(ApplicationContext.class);
    final SectionPowerAlgorithm algorithm = mock(SectionPowerAlgorithm.class);

    final PowerSectionService service = new PowerSectionService(pedestalService, supplySessionManager, eventService,
            applicationContext);

    final Socket socket = new Socket();
    final PowerSection section = new PowerSection("Meadow", new BigDecimal("32"), "meadowAlgorithm");
    final List<SupplySession> activeSessions = List.of(new SupplySession());

    @BeforeEach
    void setUp() {
        socket.setId(11L);
        socket.setSocketNo(1);
        section.setId(7L);
        when(pedestalService.findSocket("HR-BAY-P101", 1)).thenReturn(socket);
        when(supplySessionManager.getActiveSessions(section)).thenReturn(activeSessions);
        when(applicationContext.containsBean("meadowAlgorithm")).thenReturn(true);
        when(applicationContext.getBean("meadowAlgorithm", SectionPowerAlgorithm.class)).thenReturn(algorithm);
    }

    @Test
    void anUnknownSocketIsRefused() {
        when(pedestalService.findSocket("HR-BAY-P101", 1)).thenReturn(null);

        assertThat(service.isWithinSectionBudget(SOCKET_1)).isFalse();
        verifyNoInteractions(eventService);
    }

    @Test
    void aSocketWithoutSectionHasNoLimit() {
        assertThat(service.isWithinSectionBudget(SOCKET_1)).isTrue();
        verifyNoInteractions(supplySessionManager, applicationContext, eventService);
    }

    @Test
    void theAlgorithmNamedByTheSectionDecidesWithTheActiveSessionsOfTheSection() {
        socket.setPowerSection(section);
        when(algorithm.isWithinBudget(section, socket, activeSessions)).thenReturn(true);

        assertThat(service.isWithinSectionBudget(SOCKET_1)).isTrue();

        when(algorithm.isWithinBudget(section, socket, activeSessions)).thenReturn(false);
        assertThat(service.isWithinSectionBudget(SOCKET_1)).isFalse();
        verifyNoInteractions(eventService);
    }

    @Test
    void aSectionWithoutAlgorithmRefusesAndRaisesAnAlert() {
        section.setAlgorithmBeanName(" ");
        socket.setPowerSection(section);

        assertThat(service.isWithinSectionBudget(SOCKET_1)).isFalse();
        assertAlert(PowerSectionService.NO_ALGORITHM);
        verify(supplySessionManager, never()).getActiveSessions(any());
    }

    @Test
    void aSectionWithAnUnknownAlgorithmBeanRefusesAndRaisesAnAlert() {
        section.setAlgorithmBeanName("forecastAlgorithm");
        socket.setPowerSection(section);

        assertThat(service.isWithinSectionBudget(SOCKET_1)).isFalse();
        assertAlert(PowerSectionService.UNKNOWN_ALGORITHM);
    }

    @Test
    void aBeanThatIsNoSectionAlgorithmRefusesAndRaisesAnAlert() {
        socket.setPowerSection(section);
        when(applicationContext.getBean("meadowAlgorithm", SectionPowerAlgorithm.class))
                .thenThrow(new BeanNotOfRequiredTypeException("meadowAlgorithm", SectionPowerAlgorithm.class,
                        String.class));

        assertThat(service.isWithinSectionBudget(SOCKET_1)).isFalse();
        assertAlert(PowerSectionService.ALGORITHM_NOT_LOADED);
    }

    @Test
    void aFailingAlgorithmRefusesAndRaisesAnAlert() {
        socket.setPowerSection(section);
        when(algorithm.isWithinBudget(section, socket, activeSessions)).thenThrow(new IllegalStateException("boom"));

        assertThat(service.isWithinSectionBudget(SOCKET_1)).isFalse();
        assertAlert(PowerSectionService.ALGORITHM_FAILED);
    }

    @Test
    void aFailingAlertDoesNotTurnTheRefusalIntoAnAcceptance() {
        section.setAlgorithmBeanName(null);
        socket.setPowerSection(section);
        org.mockito.Mockito.doThrow(new IllegalStateException("queue full")).when(eventService).send(any());

        assertThat(service.isWithinSectionBudget(SOCKET_1)).isFalse();
    }

    private void assertAlert(String problem) {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(captor.capture());
        Event event = captor.getValue();
        assertThat(event.getType()).isEqualTo(EventType.POWER_SECTION_MISCONFIGURED);
        assertThat(PowerSectionMisconfiguredEvent.getPowerSectionId(event)).isEqualTo(7L);
        assertThat(PowerSectionMisconfiguredEvent.getProblem(event)).isEqualTo(problem);
    }
}
