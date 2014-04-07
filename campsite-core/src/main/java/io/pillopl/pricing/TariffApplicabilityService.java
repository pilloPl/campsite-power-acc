package io.pillopl.pricing;

import io.pillopl.event.DefaultTariffChangedEvent;
import io.pillopl.event.EventService;
import io.pillopl.event.TariffAppliedEvent;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.DefaultTariff;
import io.pillopl.group.attribute.DefaultTariffService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.TariffApplicabilityDao;
import io.pillopl.persistence.dao.TariffDao;
import io.pillopl.user.Userinfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Assigns tariffs to sockets, pedestals and campsites. An assignment starts at a date and is added to the history;
 * a null tariff removes the assignments planned from that date on. The tariff must be of the pedestal's market.
 */
@Service
@Transactional(readOnly = true)
public class TariffApplicabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TariffApplicabilityService.class);

    private final TariffDao tariffDao;

    private final TariffApplicabilityDao tariffApplicabilityDao;

    private final PedestalService pedestalService;

    private final GroupService groupService;

    private final DefaultTariffService defaultTariffService;

    private final EventService eventService;

    public TariffApplicabilityService(TariffDao tariffDao, TariffApplicabilityDao tariffApplicabilityDao,
                                      PedestalService pedestalService, GroupService groupService,
                                      DefaultTariffService defaultTariffService, EventService eventService) {
        this.tariffDao = tariffDao;
        this.tariffApplicabilityDao = tariffApplicabilityDao;
        this.pedestalService = pedestalService;
        this.groupService = groupService;
        this.defaultTariffService = defaultTariffService;
        this.eventService = eventService;
    }

    /**
     * All pedestals in the SITE scope of an affiliate owner get the tariff.
     */
    @Transactional
    public List<TariffApplicability> applyTariffToAffiliatePedestals(Userinfo affiliateOwner, Long tariffId,
                                                                     LocalDateTime startDate) {
        List<Long> pedestalIds = new ArrayList<>();
        for (Pedestal pedestal : pedestalService.findPedestalsByState(affiliateOwner, null, null, null).pedestals()) {
            pedestalIds.add(pedestal.getId());
        }
        return applyTariffToPedestals(pedestalIds, tariffId, startDate, affiliateOwner.getUsername());
    }

    @Transactional
    public List<TariffApplicability> applyTariffToPedestals(List<Long> pedestalIds, Long tariffId,
                                                            LocalDateTime startDate, String username) {
        LOGGER.debug(String.format("Setting tariff with id %s and start date %s for pedestals %s", tariffId,
                startDate, pedestalIds));
        List<TariffApplicability> result = new ArrayList<>();
        for (Long pedestalId : pedestalIds) {
            TariffApplicability applicability = applyTariffToPedestal(pedestalId, tariffId, startDate, username);
            if (applicability != null) {
                result.add(applicability);
            }
        }
        return result;
    }

    /**
     * @return the new assignment, null when planned assignments were removed or the pedestal does not exist
     */
    @Transactional
    public TariffApplicability applyTariffToPedestal(Long pedestalId, Long tariffId, LocalDateTime startDate,
                                                     String username) {
        checkStartDate(tariffId, startDate);

        Pedestal pedestal = pedestalService.find(pedestalId);
        if (pedestal == null || pedestal.isDeleted()) {
            LOGGER.warn(String.format("Pedestal with id %d not found", pedestalId));
            return null;
        }

        if (tariffId == null) {
            int removed = tariffApplicabilityDao.deleteByPedestalIdStartingFrom(pedestalId,
                    startDate != null ? startDate : LocalDateTime.now());
            LOGGER.info(String.format("%d planned tariff assignments of pedestal %s removed", removed, pedestal));
            eventService.send(new TariffAppliedEvent(pedestalId, null, null, startDate));
            return null;
        }

        Tariff tariff = findTariffOfMarket(tariffId, pedestal);
        TariffApplicability applicability = TariffApplicability.forPedestal(pedestal, tariff, startDate);
        applicability.setCreatedBy(username);
        applicability = tariffApplicabilityDao.save(applicability);
        eventService.send(new TariffAppliedEvent(pedestalId, null, tariffId, startDate));
        return applicability;
    }

    /**
     * @return the new assignment, null when planned assignments were removed or the socket does not exist
     */
    @Transactional
    public TariffApplicability applyTariffToSocket(Long socketId, Long tariffId, LocalDateTime startDate,
                                                   String username) {
        checkStartDate(tariffId, startDate);

        Socket socket = pedestalService.findSocket(socketId);

        if (socket == null || socket.isDeleted()) {
            LOGGER.warn(String.format("Socket with id %d not found", socketId));
            return null;
        }

        Long pedestalId = socket.getPedestal().getId();
        if (tariffId == null) {
            int removed = tariffApplicabilityDao.deleteBySocketIdStartingFrom(socketId,
                    startDate != null ? startDate : LocalDateTime.now());
            LOGGER.info(String.format("%d planned tariff assignments of socket %s removed", removed, socketId));
            eventService.send(new TariffAppliedEvent(pedestalId, socketId, null, startDate));
            return null;
        }

        Tariff tariff = findTariffOfMarket(tariffId, socket.getPedestal());
        TariffApplicability applicability = TariffApplicability.forSocket(socket, tariff, startDate);
        applicability.setCreatedBy(username);
        applicability = tariffApplicabilityDao.save(applicability);
        eventService.send(new TariffAppliedEvent(pedestalId, socketId, tariffId, startDate));
        return applicability;
    }

    /**
     * Default tariff of a campsite or section group (step 3 of the resolution).
     *
     * @param tariffId null removes the group's default
     */
    @Transactional
    public DefaultTariff setCampsiteDefaultTariff(Group group, Long tariffId, String username) {
        Tariff tariff = null;
        if (tariffId != null) {
            tariff = tariffDao.findById(tariffId).orElseThrow(() -> new NoSuchElementException(
                    "Tariff not found: " + tariffId));
        }
        DefaultTariff defaultTariff = defaultTariffService.setDefaultTariff(group, tariff);
        eventService.send(new DefaultTariffChangedEvent(group.getId(), tariffId, username));
        return defaultTariff;
    }

    /**
     * @return the assignment of the socket itself active at the time, or null
     */
    public TariffApplicability findAppliedTariff(Long socketId, LocalDateTime at) {
        LOGGER.debug(String.format("Checking applicability for socket %s", socketId));
        return tariffApplicabilityDao.findActiveTariffForSocket(socketId, at);
    }

    /**
     * @return the assignment of the whole pedestal active at the time, or null
     */
    public TariffApplicability findAppliedTariffForPedestal(Long pedestalId, LocalDateTime at) {
        return tariffApplicabilityDao.findActiveTariffForPedestal(pedestalId, at);
    }

    public List<TariffApplicability> findAssignmentHistory(Socket socket) {
        List<TariffApplicability> result = new ArrayList<>(tariffApplicabilityDao.findBySocketId(socket.getId()));
        result.addAll(tariffApplicabilityDao.findByPedestalId(socket.getPedestal().getId()));
        return result;
    }

    private static void checkStartDate(Long tariffId, LocalDateTime startDate) {
        if ((startDate == null) && (tariffId != null)) {
            LOGGER.error("StartDate for a tariff applicability must not be null");
            throw new IllegalArgumentException("StartDate for a tariff applicability must not be null");
        }
    }

    private Tariff findTariffOfMarket(Long tariffId, Pedestal pedestal) {
        Tariff tariff = tariffDao.findById(tariffId).orElseThrow(() -> new NoSuchElementException(
                "Tariff not found: " + tariffId));
        Market market = pedestal.getGroup() != null ? groupService.determineMarketByGroupId(pedestal.getGroup().getId())
                : null;
        if (market != tariff.getMarket()) {
            throw new IllegalArgumentException(String.format("Tariff '%s' of market %s cannot be used for pedestal %s "
                    + "of market %s", tariff.getName(), tariff.getMarket(), pedestal.getCode(), market));
        }
        return tariff;
    }
}
