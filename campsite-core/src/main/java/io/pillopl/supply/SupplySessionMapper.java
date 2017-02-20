package io.pillopl.supply;

import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.dto.infrastructure.PedestalCommandDTO;
import io.pillopl.dto.supply.SupplyIncidentDTO;
import io.pillopl.dto.supply.SupplySessionDTO;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;

import java.math.BigDecimal;

/**
 * Supply sessions, incidents and pedestal commands to transfer objects (the lists come from the view SQL, single
 * objects after a change from the entities).
 */
public final class SupplySessionMapper {

    private SupplySessionMapper() {
        throw new AssertionError();
    }

    /**
     * @param record the session's consumption record, null while it is active
     * @param stale  the session is silent for longer than the stale-session timeout
     */
    public static SupplySessionDTO toDTO(SupplySession session, ConsumptionRecord record, boolean stale) {
        Socket socket = session.getSocket();
        Pedestal pedestal = socket.getPedestal();
        BigDecimal stopMeter = record != null ? record.getStopMeterKwh()
                : session.isActive() ? socket.getMeterKwh() : null;
        BigDecimal kwh = record != null ? record.getKwh()
                : session.getStartMeterKwh() != null && stopMeter != null
                ? stopMeter.subtract(session.getStartMeterKwh()).max(BigDecimal.ZERO) : null;
        String guestName = record != null ? record.getGuestName()
                : session.getCard() != null && session.getCard().getGuest() != null
                ? session.getCard().getGuest().getFullName() : null;
        return new SupplySessionDTO(session.getId(), session.getTransactionId(), session.isActive(), stale,
                socket.getId(), socket.getSocketCode(), socket.getName(), socket.getPitchNo(), pedestal.getCode(),
                pedestal.getGroup() != null ? pedestal.getGroup().getId() : null,
                pedestal.getGroup() != null ? pedestal.getGroup().getDisplayName() : null,
                session.getCard() != null ? session.getCard().getCardNumber() : null, session.getCardRfid(),
                session.getHouseCard() != null && session.getHouseCard().getHouseCardType() != null
                        ? session.getHouseCard().getHouseCardType().name() : null,
                session.getQrPayProcessId(), session.getStayId(), guestName, session.getUnitPlate(),
                session.getMaxCurrentA(), session.getRemoteDate() != null ? session.getRemoteDate()
                : session.getCreateDate(), session.getStopDate(),
                session.getStopReason() != null ? session.getStopReason().name() : null, session.getStartMeterKwh(),
                stopMeter, kwh, record != null ? record.getId() : null, socket.getModDate());
    }

    public static SupplyIncidentDTO toDTO(SupplyIncident incident, Long campsiteGroupId) {
        return new SupplyIncidentDTO(incident.getId(), incident.getIncidentType().name(), incident.getPedestalCode(),
                campsiteGroupId, incident.getTransactionId(), incident.getCardRfid(), incident.getSupplySessionId(),
                incident.getMeterKwh(), incident.getRemoteDate(), incident.getMessage(), incident.getCreateDate(),
                incident.getReviewedBy(), incident.getReviewDate(), incident.getReviewComment());
    }

    /**
     * A command the pedestal accepted (or a local action), with the socket's status after the call.
     */
    public static PedestalCommandDTO toCommand(String command, Socket socket, String message) {
        return new PedestalCommandDTO(command, socket.getPedestal().getCode(), socket.getId(), socket.getSocketNo(),
                true, socket.getStatus() != null ? socket.getStatus().name() : null, message);
    }
}
