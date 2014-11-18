package io.pillopl.power;

import io.pillopl.infrastructure.Socket;
import io.pillopl.supply.SupplySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Sum of the rated currents: the breaker currents the active sessions started with, plus the breaker current of the
 * asked socket, must not exceed the section's maximum current. A session already running on the asked socket is not
 * counted (the new supply replaces it, it does not come on top).
 */
@Component(NominalCurrentSumAlgorithm.BEAN_NAME)
public class NominalCurrentSumAlgorithm implements SectionPowerAlgorithm {

    public static final String BEAN_NAME = "nominalCurrentSum";

    private static final Logger LOG = LoggerFactory.getLogger(NominalCurrentSumAlgorithm.class);

    @Override
    public boolean isWithinBudget(PowerSection section, Socket socket, List<SupplySession> activeSessions) {
        int allocated = 0;
        for (SupplySession session : activeSessions) {
            if (!Objects.equals(session.getSocket().getId(), socket.getId())) {
                allocated += session.getMaxCurrentA();
            }
        }
        int requested = allocated + socket.getMaxCurrentInAmperes();
        boolean within = BigDecimal.valueOf(requested).compareTo(section.getMaxCurrentA()) <= 0;
        LOG.debug(String.format("Section %s: %s A allocated, %s A with socket %s, limit %s A: %s",
                section.getSectionName(), allocated, requested, socket, section.getMaxCurrentA(),
                within ? "within budget" : "over budget"));
        return within;
    }
}
