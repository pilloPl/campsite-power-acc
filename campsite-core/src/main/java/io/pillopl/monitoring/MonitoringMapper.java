package io.pillopl.monitoring;

import io.pillopl.dto.monitoring.CommunicationLogDTO;
import io.pillopl.dto.monitoring.FailedEventDTO;
import io.pillopl.dto.monitoring.HeartbeatTimeoutDTO;
import io.pillopl.dto.monitoring.SocketStatusLogDTO;
import io.pillopl.event.EventRetry;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalState;
import io.pillopl.infrastructure.Socket;

import java.util.List;

/**
 * Monitoring entities to the transfer objects of the monitoring and service tab.
 */
public final class MonitoringMapper {

    private MonitoringMapper() {
        throw new AssertionError();
    }

    public static SocketStatusLogDTO toDTO(SocketStatusLog log, List<String> errors) {
        Socket socket = log.getSocket();
        return new SocketStatusLogDTO(log.getId(), socket.getId(), socket.getSocketCode(), socket.getName(),
                socket.getPedestal().getCode(), log.getStatus() != null ? log.getStatus().name() : null,
                log.getMeterKwh(), log.getCurrentA(), log.getErrorFlags(), errors, log.getCreateDate(),
                log.getRemoteDate());
    }

    public static CommunicationLogDTO toDTO(PedestalCommunicationLog log) {
        return new CommunicationLogDTO(log.getId(), log.getPedestal().getCode(),
                log.getLogType() != null ? log.getLogType().name() : null, log.getDate(), log.getMessage());
    }

    public static HeartbeatTimeoutDTO toDTO(PedestalState state) {
        Pedestal pedestal = state.getPedestal();
        return new HeartbeatTimeoutDTO(pedestal.getId(), pedestal.getCode(),
                pedestal.getGroup() != null ? pedestal.getGroup().getId() : null,
                pedestal.getGroup() != null ? pedestal.getGroup().getDisplayName() : null,
                pedestal.getPedestalType() != null && pedestal.getPedestalType().getLinkVersion() != null
                        ? pedestal.getPedestalType().getLinkVersion().name() : null,
                state.getApproxLastIncoming(), state.getLastTimeout());
    }

    public static FailedEventDTO toDTO(EventRetry retry) {
        return new FailedEventDTO(retry.getId(), retry.getEventId(),
                retry.getEventType() != null ? retry.getEventType().name() : null, retry.getAttributes(),
                retry.getFailure(), retry.getRetryCount(), retry.getCreateDate(), retry.getLastRetryDate());
    }
}
