package io.pillopl.infrastructure;

/**
 * Filter of the pedestal list in the console.
 */
public enum PedestalListState {

    /**
     * At least one socket is not monitored.
     */
    DISABLED,

    /**
     * A monitored socket reports an error, or the link timed out, or the pedestal never sent anything.
     */
    ERROR,

    /**
     * Monitored, no socket in error, link alive.
     */
    WORKING
}
