package io.pillopl.power;

import io.pillopl.value.SocketLinkId;
import org.springframework.stereotype.Component;

/**
 * Budget used while no power sections are configured: a socket without section has no limit.
 */
@Component
public class UnlimitedSectionPowerBudget implements SectionPowerBudget {

    @Override
    public boolean isWithinSectionBudget(SocketLinkId socketLinkId) {
        return true;
    }
}
