package io.pillopl.power;

import io.pillopl.infrastructure.Socket;
import io.pillopl.supply.SupplySession;

import java.util.List;

/**
 * Check of a section switchboard's budget. Implementations are Spring beans; a section names its bean in
 * POWER_SECTION.ALGORITHM_BEAN_NAME.
 */
public interface SectionPowerAlgorithm {

    /**
     * @param section        the switchboard of the socket
     * @param socket         the socket a new supply is asked for
     * @param activeSessions the active sessions of all sockets of the section
     * @return true when the section can take the new supply
     */
    boolean isWithinBudget(PowerSection section, Socket socket, List<SupplySession> activeSessions);
}
