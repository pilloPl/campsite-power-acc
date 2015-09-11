package io.pillopl.infrastructure;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.IsLinkedToGroup;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.SocketDao;
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

    public PedestalService(PedestalDao pedestalDao, SocketDao socketDao, PedestalStateDao pedestalStateDao,
                           GroupService groupService, UserinfoService userinfoService,
                           LocationService locationService) {
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
}
