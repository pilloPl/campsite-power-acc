package io.pillopl.api;

import io.pillopl.commons.DateRange;
import io.pillopl.dto.power.PowerSectionDTO;
import io.pillopl.dto.power.SectionLoadDTO;
import io.pillopl.value.Username;

import java.math.BigDecimal;
import java.util.List;

/**
 * Power budget tab: section switchboards, their load and their configuration. Reads need PEDESTAL_VIEW, changes
 * PEDESTAL_EDIT. A section is visible when one of its sockets is in the caller's SITE scope or it has no sockets
 * yet.
 */
public interface PowerBudgetApi {

    List<PowerSectionDTO> findPowerSections(Username authUsername);

    /**
     * Names of the allocation algorithm beans a section may select.
     */
    List<String> findAllocationAlgorithms(Username authUsername);

    /**
     * Hourly load of the section in the period (at most 31 days; open ends: the last 24 hours).
     */
    SectionLoadDTO getSectionLoad(Username authUsername, Long sectionId, DateRange period);

    /**
     * Creates (sectionId null) or renames a section and sets its capacity and algorithm.
     */
    PowerSectionDTO savePowerSection(Username authUsername, Long sectionId, String sectionName, BigDecimal maxCurrentA,
                                     String algorithmBeanName);

    PowerSectionDTO changeSectionCapacity(Username authUsername, Long sectionId, BigDecimal maxCurrentA);

    PowerSectionDTO selectAllocationAlgorithm(Username authUsername, Long sectionId, String algorithmBeanName);

    /**
     * Hangs the socket on the section; sectionId null takes it off any section (no section limit).
     *
     * @return the section the socket hangs on now, null without section
     */
    PowerSectionDTO assignSocketToSection(Username authUsername, Long socketId, Long sectionId);
}
