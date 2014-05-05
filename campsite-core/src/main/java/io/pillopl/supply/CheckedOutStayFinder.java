package io.pillopl.supply;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;

/**
 * Stays whose guests checked out: their supply sessions are closed with {@link StopReason#CHECKOUT}. Implemented by
 * the guest module once stays exist; without an implementation no session is closed on check-out.
 */
public interface CheckedOutStayFinder {

    /**
     * @param stayIds          stays that still have an active supply session
     * @param checkedOutBefore only check-outs before this time count (the guest may still unplug after check-out)
     * @return the given stays that were checked out before the time
     */
    Set<Long> findCheckedOutStayIds(Collection<Long> stayIds, LocalDateTime checkedOutBefore);
}
