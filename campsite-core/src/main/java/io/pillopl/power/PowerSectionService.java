package io.pillopl.power;

import io.pillopl.event.EventService;
import io.pillopl.event.PowerSectionMisconfiguredEvent;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.supply.SupplySession;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.value.SocketLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Power budget of the section switchboards, asked by the card authorization before a supply is allowed. The
 * section names the bean of its algorithm in the database. Fail-closed: a socket without section has explicitly no
 * limit, but a section whose budget cannot be checked (no algorithm, unknown bean, failing algorithm) refuses and
 * raises an alert.
 */
@Primary
@Service
public class PowerSectionService implements SectionPowerBudget {

    private static final Logger LOG = LoggerFactory.getLogger(PowerSectionService.class);

    static final String NO_ALGORITHM = "NO_ALGORITHM";

    static final String UNKNOWN_ALGORITHM = "UNKNOWN_ALGORITHM";

    static final String ALGORITHM_NOT_LOADED = "ALGORITHM_NOT_LOADED";

    static final String ALGORITHM_FAILED = "ALGORITHM_FAILED";

    private final PedestalService pedestalService;

    private final SupplySessionManager supplySessionManager;

    private final EventService eventService;

    private final ApplicationContext applicationContext;

    public PowerSectionService(PedestalService pedestalService, SupplySessionManager supplySessionManager,
                               EventService eventService, ApplicationContext applicationContext) {
        this.pedestalService = pedestalService;
        this.supplySessionManager = supplySessionManager;
        this.eventService = eventService;
        this.applicationContext = applicationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWithinSectionBudget(final SocketLinkId socketLinkId) {
        LOG.debug(String.format("Section budget: is there power for socket %s", socketLinkId));
        Socket socket = pedestalService.findSocket(socketLinkId.getPedestalCode(), socketLinkId.socketNo());
        if (socket == null) {
            LOG.warn(String.format("Section budget: socket %s unknown, supply refused", socketLinkId));
            return false;
        }
        PowerSection section = socket.getPowerSection();
        if (section == null) {
            LOG.debug(String.format("Section budget: socket %s hangs on no section, no limit", socketLinkId));
            return true;
        }
        SectionPowerAlgorithm algorithm = getAlgorithm(section, socket);
        if (algorithm == null) {
            return false;
        }
        List<SupplySession> activeSessions = supplySessionManager.getActiveSessions(section);
        try {
            boolean within = algorithm.isWithinBudget(section, socket, activeSessions);
            LOG.debug(String.format("Section budget: socket %s in section %s with %s active sessions: %s",
                    socketLinkId, section.getSectionName(), activeSessions.size(), within));
            return within;
        } catch (RuntimeException e) {
            LOG.error(String.format("Section budget: algorithm %s of section %s failed for socket %s",
                    section.getAlgorithmBeanName(), section, socketLinkId), e);
            alert(section, socket, ALGORITHM_FAILED);
            return false;
        }
    }

    /**
     * @return the bean named by the section, or null (after an alert) when there is none of the right type
     */
    SectionPowerAlgorithm getAlgorithm(final PowerSection section, final Socket socket) {
        if (!section.hasAlgorithm()) {
            LOG.error(String.format("Section budget: section %s has no algorithm, supplies refused", section));
            alert(section, socket, NO_ALGORITHM);
            return null;
        }
        String beanName = section.getAlgorithmBeanName();
        if (!applicationContext.containsBean(beanName)) {
            LOG.error(String.format("Section budget: algorithm bean %s of section %s not found, supplies refused",
                    beanName, section));
            alert(section, socket, UNKNOWN_ALGORITHM);
            return null;
        }
        try {
            return applicationContext.getBean(beanName, SectionPowerAlgorithm.class);
        } catch (BeansException e) {
            LOG.error(String.format("Section budget: bean %s of section %s is no section algorithm, supplies refused",
                    beanName, section), e);
            alert(section, socket, ALGORITHM_NOT_LOADED);
            return null;
        }
    }

    private void alert(final PowerSection section, final Socket socket, final String problem) {
        try {
            eventService.send(new PowerSectionMisconfiguredEvent(section.getId(), socket.getId(),
                    socket.getPedestal() != null ? socket.getPedestal().getCode() : null, socket.getSocketNo(),
                    problem));
        } catch (Exception e) {
            LOG.error(String.format("Section budget: alert %s for section %s could not be sent", problem, section), e);
        }
    }
}
