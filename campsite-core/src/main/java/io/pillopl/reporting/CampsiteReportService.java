package io.pillopl.reporting;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.report.ReportLineDTO;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.ConsumptionReportDao;
import io.pillopl.persistence.dao.ConsumptionReportDao.ReportRecord;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.SupplyIncidentDao;
import io.pillopl.supply.SupplyIncident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Management reports of the campsites: average consumption per hour of the day and per day of the week, consumption
 * per month and quarter, kWh per stay night, top pitches, supply incidents per pedestal.
 * <p>
 * Every report reads the scope of its reader: the SITE subtree {@code siteGroupId} (a campsite or the whole operator)
 * and/or the ISSUER subtree {@code issuerGroupId}; a null id makes that side empty. A power section narrows the
 * consumption reports to the sockets of that section switchboard.
 */
@Service
@Transactional(readOnly = true)
public class CampsiteReportService {

    private static final Logger LOG = LoggerFactory.getLogger(CampsiteReportService.class);

    static final int KWH_SCALE = 3;

    private static final int SLICE_SCALE = 6;

    private static final int HOURS_PER_DAY = 24;

    private final ConsumptionReportDao consumptionReportDao;

    private final SupplyIncidentDao supplyIncidentDao;

    private final PedestalDao pedestalDao;

    private final SystemPropertyService systemPropertyService;

    public CampsiteReportService(ConsumptionReportDao consumptionReportDao, SupplyIncidentDao supplyIncidentDao,
                                 PedestalDao pedestalDao, SystemPropertyService systemPropertyService) {
        this.consumptionReportDao = consumptionReportDao;
        this.supplyIncidentDao = supplyIncidentDao;
        this.pedestalDao = pedestalDao;
        this.systemPropertyService = systemPropertyService;
    }

    /**
     * Average kWh a socket draws in each day of the week (Monday first). A record is spread evenly over its supply
     * time; the part falling into a day goes to that day's total. The total of a day is divided by the number of
     * sockets that drew energy on that day.
     *
     * @return seven lines MONDAY..SUNDAY: kWh = average per socket, count = sockets
     */
    public List<ReportLineDTO> weekdayConsumptionProfile(LocalDateTime from, LocalDateTime to,
                                                                 Long siteGroupId, Long issuerGroupId,
                                                                 Long powerSectionId) {
        List<ReportLineDTO> lines = new ArrayList<>();

        // First, all records of the period.
        List<ReportRecord> records = consumptionReportDao.findReportRecords(from, to, siteGroupId, issuerGroupId,
                powerSectionId);

        // Helper maps for each day of the week: kWh total and the sockets that drew energy on that day.
        Map<DayOfWeek, BigDecimal> totalConsumptionPerDay = new HashMap<>();
        Map<DayOfWeek, Set<String>> socketsPerDay = new HashMap<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            totalConsumptionPerDay.put(dayOfWeek, BigDecimal.ZERO);
            socketsPerDay.put(dayOfWeek, new HashSet<>());
        }

        // For each record, determine how much it contributes to each day.
        for (ReportRecord record : records) {
            LocalDateTime plugIn = record.plugIn();
            LocalDateTime plugOut = record.plugOut();
            BigDecimal kwh = record.kwh();
            if (plugIn == null || plugOut == null || kwh == null) {
                continue;
            }

            long supplySeconds = Duration.between(plugIn, plugOut).getSeconds();
            if (supplySeconds <= 0) {
                // nothing to spread: an instant (or a confirmed suspect record with negative duration)
                LOG.debug(String.format("Record of socket %s without supply time left out of the day report",
                        record.socketCode()));
                continue;
            }
            if (kwh.signum() <= 0) {
                continue;
            }
            String socket = record.socketCode() != null ? record.socketCode() : record.pedestalCode();

            // Iterate per day from the start of the plug-in day until crossing the plug-out.
            LocalDateTime day = plugIn.truncatedTo(ChronoUnit.DAYS);
            while (day.isBefore(plugOut)) {
                LocalDateTime nextDay = day.plusDays(1);

                // Portion of this day included in the supply.
                LocalDateTime sliceStart = day.isBefore(plugIn) ? plugIn : day;
                LocalDateTime sliceEnd = nextDay.isAfter(plugOut) ? plugOut : nextDay;
                long secondsThisDay = Duration.between(sliceStart, sliceEnd).getSeconds();

                if (secondsThisDay > 0) {
                    BigDecimal consumptionThisDay = kwh.multiply(BigDecimal.valueOf(secondsThisDay))
                            .divide(BigDecimal.valueOf(supplySeconds), SLICE_SCALE, RoundingMode.HALF_UP);

                    DayOfWeek dayOfWeek = day.getDayOfWeek();
                    BigDecimal consumption = totalConsumptionPerDay.get(dayOfWeek);
                    consumption = consumption.add(consumptionThisDay);
                    totalConsumptionPerDay.put(dayOfWeek, consumption);

                    // The socket counts for the day only when this record drew energy on it.
                    if (consumptionThisDay.signum() > 0) {
                        socketsPerDay.get(dayOfWeek).add(socket);
                    }
                }
                day = nextDay;
            }
        }

        // Average per socket of each day.
        for (Map.Entry<DayOfWeek, BigDecimal> entry : totalConsumptionPerDay.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey();
            BigDecimal consumption = entry.getValue();
            Set<String> sockets = socketsPerDay.get(dayOfWeek);
            if (!sockets.isEmpty()) {
                consumption = consumption.divide(BigDecimal.valueOf(sockets.size()), SLICE_SCALE,
                        RoundingMode.HALF_UP);
                totalConsumptionPerDay.put(dayOfWeek, consumption);
            }
        }

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            lines.add(dayLine(dayOfWeek, totalConsumptionPerDay, socketsPerDay));
        }

        return lines;
    }

    /**
     * Average kWh a socket draws in each hour of the day (00..23). A record is spread evenly over its supply time;
     * the part falling into an hour goes to that hour's total. The total of an hour is divided by the number of
     * sockets that drew energy in that hour.
     *
     * @return 24 lines "00".."23": kWh = average per socket, count = sockets
     */
    public List<ReportLineDTO> hourlyConsumptionProfile(LocalDateTime from, LocalDateTime to,
                                                                 Long siteGroupId, Long issuerGroupId,
                                                                 Long powerSectionId) {
        List<ReportLineDTO> lines = new ArrayList<>();

        // First, all records of the period.
        List<ReportRecord> records = consumptionReportDao.findReportRecords(from, to, siteGroupId, issuerGroupId,
                powerSectionId);

        // Helper maps for each hour of the day: kWh total and the sockets that drew energy in that hour.
        Map<Integer, BigDecimal> totalConsumptionPerHour = new HashMap<>();
        Map<Integer, Set<String>> socketsPerHour = new HashMap<>();
        for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
            totalConsumptionPerHour.put(hour, BigDecimal.ZERO);
            socketsPerHour.put(hour, new HashSet<>());
        }

        // For each record, determine how much it contributes to each hour.
        for (ReportRecord record : records) {
            LocalDateTime plugIn = record.plugIn();
            LocalDateTime plugOut = record.plugOut();
            BigDecimal kwh = record.kwh();
            if (plugIn == null || plugOut == null || kwh == null) {
                continue;
            }

            long supplySeconds = Duration.between(plugIn, plugOut).getSeconds();
            if (supplySeconds <= 0) {
                // nothing to spread: an instant (or a confirmed suspect record with negative duration)
                LOG.debug(String.format("Record of socket %s without supply time left out of the hour report",
                        record.socketCode()));
                continue;
            }
            if (kwh.signum() <= 0) {
                continue;
            }
            String socket = record.socketCode() != null ? record.socketCode() : record.pedestalCode();

            // Iterate per hour from the start of the plug-in hour until crossing the plug-out.
            LocalDateTime hourStart = plugIn.truncatedTo(ChronoUnit.HOURS);
            while (hourStart.isBefore(plugOut)) {
                LocalDateTime nextHour = hourStart.plusHours(1);

                // Portion of this hour included in the supply.
                LocalDateTime sliceStart = hourStart.isBefore(plugIn) ? plugIn : hourStart;
                LocalDateTime sliceEnd = nextHour.isAfter(plugOut) ? plugOut : nextHour;
                long secondsThisHour = Duration.between(sliceStart, sliceEnd).getSeconds();

                if (secondsThisHour > 0) {
                    BigDecimal consumptionThisHour = kwh.multiply(BigDecimal.valueOf(secondsThisHour))
                            .divide(BigDecimal.valueOf(supplySeconds), SLICE_SCALE, RoundingMode.HALF_UP);

                    Integer hour = hourStart.getHour();
                    BigDecimal consumption = totalConsumptionPerHour.get(hour);
                    consumption = consumption.add(consumptionThisHour);
                    totalConsumptionPerHour.put(hour, consumption);

                    // The socket counts for the hour only when this record drew energy in it.
                    if (consumptionThisHour.signum() > 0) {
                        socketsPerHour.get(hour).add(socket);
                    }
                }
                hourStart = nextHour;
            }
        }

        // Average per socket of each hour.
        for (Map.Entry<Integer, BigDecimal> entry : totalConsumptionPerHour.entrySet()) {
            Integer hour = entry.getKey();
            BigDecimal consumption = entry.getValue();
            Set<String> sockets = socketsPerHour.get(hour);
            if (!sockets.isEmpty()) {
                consumption = consumption.divide(BigDecimal.valueOf(sockets.size()), SLICE_SCALE,
                        RoundingMode.HALF_UP);
                totalConsumptionPerHour.put(hour, consumption);
            }
        }

        for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
            lines.add(new ReportLineDTO(String.format("%02d", hour), null,
                    totalConsumptionPerHour.get(hour).setScale(KWH_SCALE, RoundingMode.HALF_UP),
                    socketsPerHour.get(hour).size(), null));
        }

        return lines;
    }

    /**
     * @param year  null for every year
     * @param month 1..12, null for every month
     * @return kWh, records and supply hours per month of plug-out ("2026_07"), oldest first
     */
    public List<ReportLineDTO> getMonthlyConsumptions(Long siteGroupId, Long issuerGroupId, Long powerSectionId,
                                                      Integer year, Integer month) {
        return consumptionReportDao.getMonthlyConsumptions(siteGroupId, issuerGroupId, powerSectionId, year, month);
    }

    /**
     * @param year null for every year
     * @return kWh, records and supply hours per quarter of plug-out ("2026_Q3"), oldest first
     */
    public List<ReportLineDTO> getQuarterlyConsumptions(Long siteGroupId, Long issuerGroupId, Long powerSectionId,
                                                        Integer year) {
        return consumptionReportDao.getQuarterlyConsumptions(siteGroupId, issuerGroupId, powerSectionId, year);
    }

    /**
     * kWh per stay night of each campsite: the energy of the records divided by their nights.
     *
     * @return one line per campsite code: kWh = kWh per night, count = nights
     */
    public List<ReportLineDTO> getConsumptionPerStayNight(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                          Long issuerGroupId, Long powerSectionId) {
        List<ReportLineDTO> lines = new ArrayList<>();
        for (ReportLineDTO campsite : consumptionReportDao.getConsumptionPerStayNight(from, to, siteGroupId,
                issuerGroupId, powerSectionId)) {
            BigDecimal kwhPerNight = campsite.kwh().divide(BigDecimal.valueOf(campsite.count()), KWH_SCALE,
                    RoundingMode.HALF_UP);
            lines.add(new ReportLineDTO(campsite.period(), null, kwhPerNight, campsite.count(), null));
        }
        return lines;
    }

    /**
     * The pitches drawing the most energy, as many as {@code ReportTopPitchesMaxRows} in SYSTEM_PROPERTY says.
     *
     * @return period = pitch number, series = campsite code, kWh and records; most kWh first
     */
    public List<ReportLineDTO> getTopPitches(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                             Long issuerGroupId, Long powerSectionId) {
        return consumptionReportDao.getTopPitches(from, to, siteGroupId, issuerGroupId, powerSectionId,
                systemPropertyService.getReportTopPitchesMaxRows());
    }

    /**
     * Stops the pedestal backend could not book (SUPPLY_INCIDENT_LOG), counted per incident type and pedestal of the
     * reader's SITE subtree. Incidents belong to the infrastructure: without a SITE group the report is empty.
     *
     * @return one line per incident type (period) and pedestal code (series), count = incidents; types and pedestals
     * in alphabetical order
     */
    public List<ReportLineDTO> getSupplyIncidentsPerPedestal(LocalDateTime from, LocalDateTime to,
                                                             Long siteGroupId) {
        List<ReportLineDTO> lines = new ArrayList<>();
        if (siteGroupId == null) {
            return lines;
        }
        Set<String> pedestalCodes = new HashSet<>();
        for (Pedestal pedestal : pedestalDao.findPedestalsByState(null, siteGroupId, null, null)) {
            pedestalCodes.add(pedestal.getCode());
        }
        Map<String, Map<String, Long>> countPerTypeAndPedestal = new TreeMap<>();
        for (SupplyIncident incident : supplyIncidentDao.findByCreateDateBetweenOrderByCreateDate(from, to)) {
            if (incident.getPedestalCode() == null || !pedestalCodes.contains(incident.getPedestalCode())) {
                continue;
            }
            Map<String, Long> perPedestal = countPerTypeAndPedestal.computeIfAbsent(
                    incident.getIncidentType().name(), type -> new TreeMap<>());
            Long count = perPedestal.get(incident.getPedestalCode());
            perPedestal.put(incident.getPedestalCode(), count == null ? 1L : count + 1);
        }
        for (Map.Entry<String, Map<String, Long>> type : countPerTypeAndPedestal.entrySet()) {
            for (Map.Entry<String, Long> pedestal : type.getValue().entrySet()) {
                lines.add(new ReportLineDTO(type.getKey(), pedestal.getKey(), null, pedestal.getValue(), null));
            }
        }
        return lines;
    }

    private static ReportLineDTO dayLine(DayOfWeek dayOfWeek, Map<DayOfWeek, BigDecimal> totalConsumptionPerDay,
                                         Map<DayOfWeek, Set<String>> socketsPerDay) {
        return new ReportLineDTO(dayOfWeek.name(), null,
                totalConsumptionPerDay.get(dayOfWeek).setScale(KWH_SCALE, RoundingMode.HALF_UP),
                socketsPerDay.get(dayOfWeek).size(), null);
    }
}
