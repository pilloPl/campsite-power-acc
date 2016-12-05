package io.pillopl.infrastructure;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.IsLinkedToGroup;
import io.pillopl.event.EventRetry;
import io.pillopl.monitoring.PedestalCommunicationLog;
import io.pillopl.monitoring.PedestalMailService;
import io.pillopl.monitoring.SocketStatusLog;
import io.pillopl.persistence.dao.EventRetryDao;
import io.pillopl.persistence.dao.PedestalCommunicationLogDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.supply.RemoteSupplyControl;
import io.pillopl.util.DateService;
import jakarta.mail.MessagingException;
import org.springframework.data.domain.PageRequest;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Pedestals and their sockets: lookups, the administrative scope of console users (SITE subtree), the pedestal list,
 * the link state, pitch assignment and soft delete.
 */
@Service
@Transactional
public class PedestalService {

    private static final Logger LOG = LoggerFactory.getLogger(PedestalService.class);

    /**
     * The link state is written only when the last incoming message moved by more than this (write load).
     */
    static final int STATE_WRITE_THRESHOLD_IN_SECONDS = 60;

    private final PedestalDao pedestalDao;

    private final SocketDao socketDao;

    private final PedestalStateDao pedestalStateDao;

    private final GroupService groupService;

    private final UserinfoService userinfoService;

    private final LocationService locationService;

    private final SocketStatusLogDao socketStatusLogDao;

    private final PedestalCommunicationLogDao pedestalCommunicationLogDao;

    private final EventRetryDao eventRetryDao;

    private final SupplySessionDao supplySessionDao;

    private final RemoteSupplyControl remoteSupplyControl;

    private final PedestalMailService pedestalMailService;

    private final DateService dateService;

    public PedestalService(PedestalDao pedestalDao, SocketDao socketDao, PedestalStateDao pedestalStateDao,
                           GroupService groupService, UserinfoService userinfoService,
                           LocationService locationService, SocketStatusLogDao socketStatusLogDao,
                           PedestalCommunicationLogDao pedestalCommunicationLogDao, EventRetryDao eventRetryDao,
                           SupplySessionDao supplySessionDao, RemoteSupplyControl remoteSupplyControl,
                           PedestalMailService pedestalMailService, DateService dateService) {
        this.socketStatusLogDao = socketStatusLogDao;
        this.pedestalCommunicationLogDao = pedestalCommunicationLogDao;
        this.eventRetryDao = eventRetryDao;
        this.supplySessionDao = supplySessionDao;
        this.remoteSupplyControl = remoteSupplyControl;
        this.pedestalMailService = pedestalMailService;
        this.dateService = dateService;
        this.pedestalDao = pedestalDao;
        this.socketDao = socketDao;
        this.pedestalStateDao = pedestalStateDao;
        this.groupService = groupService;
        this.userinfoService = userinfoService;
        this.locationService = locationService;
    }

    @Transactional(readOnly = true)
    public Pedestal find(Long id) {
        return id != null ? pedestalDao.findById(id).orElse(null) : null;
    }

    @Transactional(readOnly = true)
    public Pedestal findActive(Long id) {
        return id != null ? pedestalDao.findActiveById(id) : null;
    }

    @Transactional(readOnly = true)
    public Pedestal findByCode(String code) {
        return code != null ? pedestalDao.findByCode(code) : null;
    }

    @Transactional(readOnly = true)
    public Pedestal findBySocketId(Long socketId) {
        return pedestalDao.findBySocketId(socketId);
    }

    @Transactional(readOnly = true)
    public List<Pedestal> getByGroupIds(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return new ArrayList<>();
        }
        return pedestalDao.findByGroupIds(groupIds);
    }

    @Transactional(readOnly = true)
    public Socket findSocket(Long socketId) {
        return socketId != null ? socketDao.findById(socketId).orElse(null) : null;
    }

    @Transactional(readOnly = true)
    public Socket findSocketByCode(String socketCode) {
        return socketDao.findBySocketCode(socketCode);
    }

    /**
     * Socket as addressed on the pedestal link: pedestal code and socket number.
     */
    @Transactional(readOnly = true)
    public Socket findSocket(String pedestalCode, Integer socketNo) {
        if (pedestalCode == null || socketNo == null) {
            return null;
        }
        return socketDao.findByPedestalCodeAndSocketNo(pedestalCode, socketNo);
    }

    /**
     * A pedestal is manageable when its group lies in the user's SITE scope.
     */
    @Transactional(readOnly = true)
    public boolean isManageable(Userinfo userinfo, Pedestal pedestal) {
        return groupService.isManageable(userinfo.getGroup(GroupType.SITE), pedestal.getGroup());
    }

    @Transactional(readOnly = true)
    public boolean checkIsManageable(Userinfo userinfo, Pedestal pedestal) {
        if (!isManageable(userinfo, pedestal)) {
            throw new AccessDeniedException(String.format("User '%s' has no access to pedestal '%s'",
                    userinfo.getUsername(), pedestal.getCode()));
        }
        return true;
    }

    /**
     * @return the pedestal when it lies in the user's SITE subtree, null otherwise or when the user has no SITE scope
     */
    @Transactional(readOnly = true)
    public Pedestal findByCodeForUser(Userinfo userinfo, String code) {
        Group siteGroup = userinfo.getGroup(GroupType.SITE);
        if (siteGroup == null) {
            LOG.info(String.format("User '%s' has no SITE group", userinfo.getUsername()));
            return null;
        }
        return pedestalDao.findByCodeRestrictedToUserSiteGroup(code, siteGroup.getId());
    }

    /**
     * Pedestals of the user's SITE scope where the cards of the guest are accepted.
     */
    @Transactional(readOnly = true)
    public List<Pedestal> findPedestalsForGuest(Userinfo userinfo, IsLinkedToGroup guest) {
        Group siteGroup = userinfo.getGroup(GroupType.SITE);
        if (siteGroup == null || guest.getGroup() == null) {
            return new ArrayList<>();
        }
        return pedestalDao.findPedestalsForGuestRestrictedToUserSiteGroup(siteGroup.getId(), guest.getGroup().getId());
    }

    /**
     * The pedestal list of the console. Without paging (page or rowsPerPage null) the whole list is returned.
     */
    @Transactional(readOnly = true)
    public PedestalPage findPedestalsByState(Userinfo userinfo, PedestalListState state, Integer page,
                                             Integer rowsPerPage) {
        Group siteGroup = userinfo.getGroup(GroupType.SITE);
        if (siteGroup == null) {
            return new PedestalPage(new ArrayList<>(), 0);
        }
        if (page == null || rowsPerPage == null) {
            List<Pedestal> list = pedestalDao.findPedestalsByState(state, siteGroup.getId(), null, null);
            return new PedestalPage(list, list.size());
        }
        List<Pedestal> list = pedestalDao.findPedestalsByState(state, siteGroup.getId(), page, rowsPerPage);
        long count = pedestalDao.countPedestalsByState(state, siteGroup.getId());
        return new PedestalPage(list, count);
    }

    /**
     * Soft deletes a pedestal of an affiliate owner and all its sockets. The principal must manage the owner and
     * the owner must manage the pedestal.
     */
    public void deletePedestal(Userinfo principal, Userinfo owner, String pedestalCode) {
        userinfoService.checkIsManageable(principal, owner, false);

        Pedestal pedestal = pedestalDao.findByCode(pedestalCode);
        if (pedestal == null) {
            throw new IllegalArgumentException("Could not find pedestal with code: " + pedestalCode);
        }
        if (!groupService.isManageable(owner.getGroup(GroupType.SITE), pedestal.getGroup())) {
            throw new AccessDeniedException(String.format("User '%s' has no access to pedestal '%s'",
                    owner.getUsername(), pedestal.getCode()));
        }

        pedestal.setDeleted(true);
        for (Socket socket : pedestal.getSockets()) {
            socket.setDeleted(true);
        }
        pedestalDao.save(pedestal);
    }

    /**
     * Only updates the address yet.
     */
    public Pedestal updateLocation(Userinfo principal, Userinfo owner, String pedestalCode, Location newLocation) {
        userinfoService.checkIsManageable(principal, owner, false);

        Pedestal pedestal = pedestalDao.findByCode(pedestalCode);
        if (pedestal == null) {
            throw new IllegalArgumentException("Could not find pedestal with code: " + pedestalCode);
        }
        if (!groupService.isManageable(owner.getGroup(GroupType.SITE), pedestal.getGroup())) {
            throw new AccessDeniedException(String.format("User '%s' has no access to pedestal '%s'",
                    owner.getUsername(), pedestal.getCode()));
        }

        Location location = locationService.save(LocationService.copyOf(newLocation));
        pedestal.setLocation(location);
        return pedestalDao.save(pedestal);
    }

    /**
     * Assigns the socket to a pitch (null or blank removes the assignment). A pitch has at most one socket per
     * SITE group of the pedestal.
     */
    public Socket assignPitch(Userinfo userinfo, Long socketId, String pitchNo) {
        Socket socket = findSocket(socketId);
        if (socket == null) {
            throw new IllegalArgumentException("Could not find socket with id: " + socketId);
        }
        Pedestal pedestal = socket.getPedestal();
        checkIsManageable(userinfo, pedestal);

        if (pitchNo == null || pitchNo.isBlank()) {
            socket.setPitchNo(null);
            return socketDao.save(socket);
        }
        String normalized = pitchNo.trim().toUpperCase();
        if (pedestal.getGroup() != null) {
            for (Socket other : socketDao.findByPitchNoInGroup(normalized, pedestal.getGroup().getId())) {
                if (!other.getId().equals(socket.getId())) {
                    throw new IllegalStateException(String.format("Pitch %s already has socket %s", normalized,
                            other.getName()));
                }
            }
        }
        socket.setPitchNo(normalized);
        return socketDao.save(socket);
    }

    /**
     * A free socket supplies without a card and is never billed.
     */
    public Socket setFreeSocket(Userinfo userinfo, Long socketId, boolean freeSocket) {
        Socket socket = findSocket(socketId);
        if (socket == null) {
            throw new IllegalArgumentException("Could not find socket with id: " + socketId);
        }
        checkIsManageable(userinfo, socket.getPedestal());
        socket.setFreeSocket(freeSocket);
        return socketDao.save(socket);
    }

    /**
     * A meter reading of the socket arrived (meter readings, stop of a supply). Missing and negative readings are
     * skipped: the socket keeps its last good reading.
     *
     * @return true when the reading was stored
     */
    public boolean updateMeterReading(Socket socket, BigDecimal meterKwh, LocalDateTime at) {
        Objects.requireNonNull(socket);
        if (meterKwh == null || meterKwh.signum() < 0) {
            LOG.warn(String.format("Meter reading %s of socket %s skipped", meterKwh, socket));
            return false;
        }
        socket.setMeterKwh(meterKwh);
        socket.setModDate(at);
        socketDao.save(socket);
        return true;
    }

    /**
     * The pedestal booted: stores what its controller reports about itself.
     *
     * @return the pedestal, or null when the code is unknown or the pedestal is deleted (the boot is rejected)
     */
    public Pedestal updateBootData(String pedestalCode, String firmwareVersion, String serialNumber,
                                   String modemIccid) {
        Pedestal pedestal = pedestalCode != null ? pedestalDao.findByCode(pedestalCode) : null;
        if (pedestal == null || pedestal.isDeleted()) {
            LOG.warn(String.format("Boot of unknown pedestal %s", pedestalCode));
            return null;
        }
        if (firmwareVersion != null) {
            pedestal.setFirmwareVersion(firmwareVersion);
        }
        if (serialNumber != null) {
            pedestal.setSerialNumber(serialNumber);
        }
        if (modemIccid != null) {
            pedestal.setModemIccid(modemIccid);
        }
        return pedestalDao.save(pedestal);
    }

    /**
     * A message of the pedestal arrived: creates or moves its link state.
     *
     * @return true when the pedestal was timed out and is back now
     */
    public boolean registerIncoming(Pedestal pedestal, LocalDateTime at) {
        Objects.requireNonNull(pedestal);
        PedestalState state = pedestalStateDao.findByPedestalId(pedestal.getId());
        if (state == null) {
            pedestalStateDao.save(new PedestalState(pedestal, at));
            return false;
        }
        boolean back = state.isTimeout();
        if (back || state.getApproxLastIncoming().plusSeconds(STATE_WRITE_THRESHOLD_IN_SECONDS).isBefore(at)) {
            state.incoming(at);
            pedestalStateDao.save(state);
        }
        return back;
    }

    @Transactional(readOnly = true)
    public PedestalState findState(Long pedestalId) {
        return pedestalStateDao.findByPedestalId(pedestalId);
    }

    public Pedestal save(Pedestal pedestal) {
        return pedestalDao.save(pedestal);
    }

    // ---------------------------------------------------------------- monitoring and service (console)

    /**
     * Statuses of the sockets of a pedestal (or of one of its sockets) in [from, to], newest first.
     */
    @Transactional(readOnly = true)
    public List<SocketStatusLog> findSocketStatusLogs(Pedestal pedestal, Socket socket, LocalDateTime from,
                                                      LocalDateTime to, int maxRows) {
        if (socket != null) {
            return socketStatusLogDao.findBySocketBetween(socket.getId(), from, to, PageRequest.of(0, maxRows));
        }
        return socketStatusLogDao.findByPedestalBetween(pedestal.getId(), from, to, PageRequest.of(0, maxRows));
    }

    /**
     * Abbreviations of the error flags of a status, read with the masks of the pedestal's manufacturer.
     */
    @Transactional(readOnly = true)
    public List<String> findErrorNames(Pedestal pedestal, long errorFlags) {
        if (errorFlags == SocketStatusLog.NO_ERROR) {
            return List.of();
        }
        String manufacturer = pedestal.getPedestalType() != null ? pedestal.getPedestalType().getManufacturer()
                : "DEFAULT";
        return socketStatusLogDao.findErrorAbbreviations(manufacturer, errorFlags);
    }

    @Transactional(readOnly = true)
    public List<PedestalCommunicationLog> findCommunicationLogs(Pedestal pedestal, LocalDateTime from,
                                                                LocalDateTime to, int maxRows) {
        return pedestalCommunicationLogDao.findByPedestalBetween(pedestal.getId(), from, to,
                PageRequest.of(0, maxRows));
    }

    /**
     * Link states of the pedestals in the groups that are timed out, longest silent first.
     */
    @Transactional(readOnly = true)
    public List<PedestalState> findHeartbeatTimeouts(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return new ArrayList<>();
        }
        return pedestalStateDao.findTimedOutInGroups(groupIds);
    }

    /**
     * Events the retry gave up on, newest first (network wide, not tied to a pedestal).
     */
    @Transactional(readOnly = true)
    public List<EventRetry> findFailedEvents(int maxRows) {
        return eventRetryDao.findByStatusOrderByIdDesc(EventRetry.Status.FAILED, PageRequest.of(0, maxRows));
    }

    /**
     * Asks the pedestal for the status of one socket or of all sockets.
     *
     * @throws IllegalStateException when the pedestal did not accept the request
     */
    public void requestStatus(Pedestal pedestal, Integer socketNo) {
        if (!remoteSupplyControl.requestStatus(pedestal, socketNo)) {
            throw new IllegalStateException(String.format("Pedestal %s did not accept the status request",
                    pedestal.getCode()));
        }
        LOG.info(String.format("Status of pedestal %s socket %s requested", pedestal.getCode(),
                socketNo != null ? socketNo : "all"));
    }

    /**
     * Ends the fault of a socket. A tripped breaker of a pedestal type with remote breaker reset is switched on
     * remotely first; any other fault was repaired on site. The socket returns to AVAILABLE, or OCCUPIED while a
     * session is active, and the change is logged without error flags.
     *
     * @return true when a remote breaker reset was sent
     * @throws IllegalStateException when the socket is not in fault or the pedestal refused the reset
     */
    public boolean resetFault(Socket socket) {
        SocketStatus status = socket.getStatus();
        if (status != SocketStatus.FAULTED && status != SocketStatus.BREAKER_TRIPPED) {
            throw new IllegalStateException(String.format("Socket %s is not in fault (%s)", socket.getName(),
                    status));
        }
        Pedestal pedestal = socket.getPedestal();
        boolean remoteReset = status == SocketStatus.BREAKER_TRIPPED && pedestal.getPedestalType() != null
                && pedestal.getPedestalType().isRemoteBreakerReset();
        if (remoteReset && !remoteSupplyControl.resetBreaker(pedestal, socket.getSocketNo())) {
            throw new IllegalStateException(String.format("Pedestal %s did not accept the breaker reset of socket %s",
                    pedestal.getCode(), socket.getName()));
        }
        LocalDateTime now = dateService.getNow();
        SocketStatus newStatus = supplySessionDao.findActiveBySocket(socket).isEmpty() ? SocketStatus.AVAILABLE
                : SocketStatus.OCCUPIED;
        socket.updateStatus(newStatus, now);
        socketDao.save(socket);
        SocketStatusLog log = new SocketStatusLog(socket, now, null);
        log.setStatus(newStatus);
        log.setMeterKwh(socket.getMeterKwh());
        socketStatusLogDao.save(log);
        LOG.info(String.format("Fault %s of socket %s reset (%s), socket is %s", status, socket,
                remoteReset ? "remote breaker reset" : "repaired on site", newStatus));
        return remoteReset;
    }

    /**
     * Mails the last fault of the socket (its last status with error flags) to the service recipients again.
     *
     * @return the error flags mailed
     * @throws IllegalStateException when the socket has no logged fault or the mail could not be sent
     */
    @Transactional(readOnly = true)
    public long resendFaultMail(Socket socket) {
        SocketStatusLog fault = socketStatusLogDao.findLastFaultBySocketId(socket.getId(), PageRequest.of(0, 1))
                .stream().findFirst().orElseThrow(() -> new IllegalStateException(String.format(
                        "Socket %s has no logged fault", socket.getName())));
        try {
            pedestalMailService.sendErrorMail(socket, fault.getErrorFlags());
        } catch (MessagingException e) {
            throw new IllegalStateException("Fault mail not sent: " + e.getMessage(), e);
        }
        return fault.getErrorFlags();
    }
}
