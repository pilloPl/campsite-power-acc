package io.pillopl.power;

import io.pillopl.api.PowerBudgetApi;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.TextUtil;
import io.pillopl.dto.power.PowerSectionDTO;
import io.pillopl.dto.power.SectionLoadDTO;
import io.pillopl.event.EventService;
import io.pillopl.event.PowerSectionMisconfiguredEvent;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.PowerSectionDao;
import io.pillopl.persistence.dao.PowerSectionDaoCustom.SectionLoadRow;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.supply.SupplySession;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.DateService;
import io.pillopl.value.SocketLinkId;
import io.pillopl.value.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Power budget of the section switchboards, asked by the card authorization before a supply is allowed. The
 * section names the bean of its algorithm in the database. Fail-closed: a socket without section has explicitly no
 * limit, but a section whose budget cannot be checked (no algorithm, unknown bean, failing algorithm) refuses and
 * raises an alert.
 * <p>
 * Also the power budget tab of the console ({@link PowerBudgetApi}): sections with their sockets and allocation, the
 * load per hour, capacity, algorithm and the sockets of a section. A capacity change only changes the limit; the
 * check of the next supply stays fail-closed (a section without a valid algorithm refuses).
 */
@Primary
@Service
public class PowerSectionService implements SectionPowerBudget, PowerBudgetApi {

    private static final Logger LOG = LoggerFactory.getLogger(PowerSectionService.class);

    static final String NO_ALGORITHM = "NO_ALGORITHM";

    static final String UNKNOWN_ALGORITHM = "UNKNOWN_ALGORITHM";

    static final String ALGORITHM_NOT_LOADED = "ALGORITHM_NOT_LOADED";

    static final String ALGORITHM_FAILED = "ALGORITHM_FAILED";

    private final PedestalService pedestalService;

    private final SupplySessionManager supplySessionManager;

    private final EventService eventService;

    private final ApplicationContext applicationContext;

    private final PowerSectionDao powerSectionDao;

    private final SocketDao socketDao;

    private final GroupService groupService;

    private final PrincipalResolver principalResolver;

    private final DateService dateService;

    static final long MAX_LOAD_DAYS = 31;

    static final int MAX_SECTION_NAME_LENGTH = 128;

    public PowerSectionService(PedestalService pedestalService, SupplySessionManager supplySessionManager,
                               EventService eventService, ApplicationContext applicationContext,
                               PowerSectionDao powerSectionDao, SocketDao socketDao, GroupService groupService,
                               PrincipalResolver principalResolver, DateService dateService) {
        this.pedestalService = pedestalService;
        this.supplySessionManager = supplySessionManager;
        this.eventService = eventService;
        this.applicationContext = applicationContext;
        this.powerSectionDao = powerSectionDao;
        this.socketDao = socketDao;
        this.groupService = groupService;
        this.principalResolver = principalResolver;
        this.dateService = dateService;
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

    // ---------------------------------------------------------------- power budget tab of the console

    @Override
    @Transactional(readOnly = true)
    public List<PowerSectionDTO> findPowerSections(Username authUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            List<PowerSectionDTO> result = new ArrayList<>();
            for (PowerSection section : powerSectionDao.findAllByOrderBySectionName()) {
                List<Socket> sockets = socketDao.findActiveByPowerSectionId(section.getId());
                if (isVisible(principal, sockets)) {
                    result.add(toDTO(section, sockets));
                }
            }
            return result;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllocationAlgorithms(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            return allocationAlgorithms();
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SectionLoadDTO getSectionLoad(Username authUsername, Long sectionId, DateRange period) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            PowerSection section = findVisibleSection(principal, sectionId);
            LocalDateTime[] range = loadRange(period, dateService.getNow());
            List<SectionLoadDTO.Point> points = new ArrayList<>();
            for (SectionLoadRow row : powerSectionDao.findSectionLoadPerHour(section.getId(), range[0], range[1])) {
                points.add(new SectionLoadDTO.Point(row.hour(), row.allocatedA(), row.measuredA(), row.sessions()));
            }
            return new SectionLoadDTO(section.getId(), section.getSectionName(), section.getMaxCurrentA(), points);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public PowerSectionDTO savePowerSection(Username authUsername, Long sectionId, String sectionName,
                                            BigDecimal maxCurrentA, String algorithmBeanName) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            String name = TextUtil.trimToNull(sectionName);
            if (name == null || name.length() > MAX_SECTION_NAME_LENGTH) {
                throw new InvalidInputException("Section name must have 1 to " + MAX_SECTION_NAME_LENGTH
                        + " characters");
            }
            PowerSection sameName = powerSectionDao.findBySectionName(name);
            PowerSection section;
            if (sectionId == null) {
                if (sameName != null) {
                    throw new ConflictException("Section " + name + " exists already");
                }
                section = new PowerSection(name, checkedCapacity(maxCurrentA), checkedAlgorithm(algorithmBeanName));
            } else {
                section = findVisibleSection(principal, sectionId);
                if (sameName != null && !sameName.getId().equals(section.getId())) {
                    throw new ConflictException("Section " + name + " exists already");
                }
                section.setSectionName(name);
                section.setMaxCurrentA(checkedCapacity(maxCurrentA));
                section.setAlgorithmBeanName(checkedAlgorithm(algorithmBeanName));
            }
            section = powerSectionDao.saveAndFlush(section);
            LOG.info(String.format("Power section %s saved by %s", section, principal.getUsername()));
            return toDTO(section, socketDao.findActiveByPowerSectionId(section.getId()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public PowerSectionDTO changeSectionCapacity(Username authUsername, Long sectionId, BigDecimal maxCurrentA) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            PowerSection section = findVisibleSection(principal, sectionId);
            BigDecimal capacity = checkedCapacity(maxCurrentA);
            int allocated = allocatedA(supplySessionManager.getActiveSessions(section));
            if (capacity.compareTo(BigDecimal.valueOf(allocated)) < 0) {
                // running supplies keep running; no new supply starts until the section is within its budget
                LOG.warn(String.format("Capacity of section %s lowered to %s A below the %s A allocated now by %s",
                        section.getSectionName(), capacity, allocated, principal.getUsername()));
            }
            LOG.info(String.format("Capacity of section %s: %s A -> %s A by %s", section.getSectionName(),
                    section.getMaxCurrentA(), capacity, principal.getUsername()));
            section.setMaxCurrentA(capacity);
            return toDTO(powerSectionDao.saveAndFlush(section), socketDao.findActiveByPowerSectionId(section.getId()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public PowerSectionDTO selectAllocationAlgorithm(Username authUsername, Long sectionId, String algorithmBeanName) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            PowerSection section = findVisibleSection(principal, sectionId);
            section.setAlgorithmBeanName(checkedAlgorithm(algorithmBeanName));
            LOG.info(String.format("Section %s allocates with %s, selected by %s", section.getSectionName(),
                    section.getAlgorithmBeanName(), principal.getUsername()));
            return toDTO(powerSectionDao.saveAndFlush(section), socketDao.findActiveByPowerSectionId(section.getId()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public PowerSectionDTO assignSocketToSection(Username authUsername, Long socketId, Long sectionId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            Socket socket = socketId != null ? socketDao.findById(socketId).orElse(null) : null;
            if (socket == null || socket.isDeleted() || !isInScope(principal, socket)) {
                throw ObjectNotFoundException.of("Socket", socketId);
            }
            PowerSection section = sectionId != null ? findVisibleSection(principal, sectionId) : null;
            LOG.info(String.format("Socket %s hangs on section %s (was %s), set by %s", socket.getSocketCode(),
                    section, socket.getPowerSection(), principal.getUsername()));
            socket.setPowerSection(section);
            socketDao.saveAndFlush(socket);
            return section != null ? toDTO(section, socketDao.findActiveByPowerSectionId(section.getId())) : null;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    /**
     * Capacity of a section: positive, at most the column's 6 integer digits, scale 2.
     */
    static BigDecimal checkedCapacity(BigDecimal maxCurrentA) {
        if (maxCurrentA == null || maxCurrentA.signum() <= 0) {
            throw new InvalidInputException("Capacity must be a positive current in A");
        }
        if (maxCurrentA.compareTo(new BigDecimal("999999.99")) > 0 || maxCurrentA.scale() > 2) {
            throw new InvalidInputException("Capacity must be below 1000000 A with at most 2 decimals");
        }
        return maxCurrentA.setScale(2);
    }

    /**
     * Only a bean that is a section algorithm can be selected: an empty or unknown name would make the section
     * refuse every supply.
     */
    String checkedAlgorithm(String algorithmBeanName) {
        String beanName = TextUtil.trimToNull(algorithmBeanName);
        if (beanName == null) {
            throw new InvalidInputException("Allocation algorithm is missing");
        }
        if (!allocationAlgorithms().contains(beanName)) {
            throw new InvalidInputException("Unknown allocation algorithm " + beanName + ", available: "
                    + allocationAlgorithms());
        }
        return beanName;
    }

    List<String> allocationAlgorithms() {
        return applicationContext.getBeansOfType(SectionPowerAlgorithm.class).keySet().stream().sorted().toList();
    }

    /**
     * [from, to) of the load: the last 24 hours when the period is open, at most {@link #MAX_LOAD_DAYS} days.
     */
    static LocalDateTime[] loadRange(DateRange period, LocalDateTime now) {
        LocalDateTime to = period != null && period.to() != null ? period.to() : now;
        LocalDateTime from = period != null && period.from() != null ? period.from() : to.minusHours(24);
        if (!from.isBefore(to)) {
            throw new InvalidInputException("The period of the load is empty");
        }
        if (Duration.between(from, to).toDays() > MAX_LOAD_DAYS) {
            throw new InvalidInputException("The load is shown for at most " + MAX_LOAD_DAYS + " days");
        }
        return new LocalDateTime[] {from, to};
    }

    private PowerSection findVisibleSection(Userinfo principal, Long sectionId) {
        PowerSection section = sectionId != null ? powerSectionDao.findById(sectionId).orElse(null) : null;
        if (section == null || !isVisible(principal, socketDao.findActiveByPowerSectionId(section.getId()))) {
            throw ObjectNotFoundException.of("Power section", sectionId);
        }
        return section;
    }

    /**
     * A section without sockets is visible to everybody with the right (it is being set up), otherwise one of its
     * sockets must be in the SITE scope of the user.
     */
    private boolean isVisible(Userinfo principal, List<Socket> sockets) {
        if (sockets.isEmpty()) {
            return true;
        }
        for (Socket socket : sockets) {
            if (isInScope(principal, socket)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInScope(Userinfo principal, Socket socket) {
        Group siteGroup = principal.getGroup(GroupType.SITE);
        return siteGroup != null && socket.getPedestal() != null
                && groupService.isManageable(siteGroup, socket.getPedestal().getGroup());
    }

    private PowerSectionDTO toDTO(PowerSection section, List<Socket> sockets) {
        List<SupplySession> activeSessions = supplySessionManager.getActiveSessions(section);
        Map<Long, Integer> allocatedPerSocket = new HashMap<>();
        for (SupplySession session : activeSessions) {
            allocatedPerSocket.merge(session.getSocket().getId(), session.getMaxCurrentA(), Integer::sum);
        }
        List<PowerSectionDTO.SocketAllocation> allocations = new ArrayList<>();
        for (Socket socket : sockets) {
            allocations.add(new PowerSectionDTO.SocketAllocation(socket.getId(), socket.getSocketCode(),
                    socket.getPedestal() != null ? socket.getPedestal().getCode() : null, socket.getSocketNo(),
                    socket.getPitchNo(), socket.getStatus() != null ? socket.getStatus().name() : null,
                    socket.getMaxCurrentInAmperes(), allocatedPerSocket.getOrDefault(socket.getId(), 0)));
        }
        boolean algorithmAvailable = section.hasAlgorithm()
                && allocationAlgorithms().contains(section.getAlgorithmBeanName());
        return new PowerSectionDTO(section.getId(), section.getSectionName(), section.getMaxCurrentA(),
                section.getAlgorithmBeanName(), algorithmAvailable, sockets.size(), allocatedA(activeSessions),
                activeSessions.size(), allocations);
    }

    static int allocatedA(List<SupplySession> activeSessions) {
        int allocated = 0;
        for (SupplySession session : activeSessions) {
            allocated += session.getMaxCurrentA();
        }
        return allocated;
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
