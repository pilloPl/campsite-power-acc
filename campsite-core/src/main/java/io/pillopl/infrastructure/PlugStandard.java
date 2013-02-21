package io.pillopl.infrastructure;

/**
 * Plug standard of a socket on a pedestal.
 */
public enum PlugStandard {

    /**
     * IEC 60309 blue, 230 V single phase, the usual campsite hook-up.
     */
    CEE_BLUE_16A,

    /**
     * IEC 60309 red, 400 V three phase, for large motorhomes and seasonal pitches.
     */
    CEE_RED_32A,

    /**
     * Household socket, mostly on tent pitches.
     */
    SCHUKO
}
