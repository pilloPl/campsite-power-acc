package io.pillopl.issuer;

import io.pillopl.card.CampingCard;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ConsumptionRecordFactory;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.SettlementFlags;
import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.attribute.IssuerTenantService;
import io.pillopl.group.attribute.SiteTenantService;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.Location;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.pricing.PricingInput;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Import of the consumption records an issuer partner sends: network card holders supplied at the partner's
 * externally managed campsites. A record is accepted only from a socket in the partner's SITE subtree and for a
 * card the network knows; it becomes a {@link ConsumptionRecord} without a supply session, with the partner as
 * source and the partner's price, and goes to the ERP billing like the network's own records.
 */
@Service
public class IssuerExchangeImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssuerExchangeImportService.class);

    static final String IMPORTED_WITHOUT_COST = "Imported without cost";

    private final ConsumptionRecordDao consumptionRecordDao;

    private final ConsumptionRecordFactory consumptionRecordFactory;

    private final IssuerPartnerDao issuerPartnerDao;

    private final CampingCardDao campingCardDao;

    private final SocketDao socketDao;

    private final GroupService groupService;

    private final SiteTenantService siteTenantService;

    private final IssuerTenantService issuerTenantService;

    private final DateService dateService;

    public IssuerExchangeImportService(ConsumptionRecordDao consumptionRecordDao,
                                       ConsumptionRecordFactory consumptionRecordFactory,
                                       IssuerPartnerDao issuerPartnerDao, CampingCardDao campingCardDao,
                                       SocketDao socketDao, GroupService groupService,
                                       SiteTenantService siteTenantService, IssuerTenantService issuerTenantService,
                                       DateService dateService) {
        this.consumptionRecordDao = consumptionRecordDao;
        this.consumptionRecordFactory = consumptionRecordFactory;
        this.issuerPartnerDao = issuerPartnerDao;
        this.campingCardDao = campingCardDao;
        this.socketDao = socketDao;
        this.groupService = groupService;
        this.siteTenantService = siteTenantService;
        this.issuerTenantService = issuerTenantService;
        this.dateService = dateService;
    }

    /**
     * Creates the consumption records of the partner's lines that are not imported yet.
     *
     * @param partnerId  partner the records come from; its SITE group limits the accepted sockets
     * @param records    the partner's lines
     * @param sourceName identifies the partner on the imported records
     * @return the lines accepted (new or imported before)
     */
    @Transactional
    public List<ConsumptionRecordDTO> importConsumptionRecords(Long partnerId, List<ConsumptionRecordDTO> records,
                                                               String sourceName) {
        if (partnerId == null || records == null || sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("Partner, records and source name are required");
        }
        IssuerPartner partner = issuerPartnerDao.findById(partnerId).orElse(null);
        if (partner == null || partner.getSiteGroup() == null) {
            throw new IllegalArgumentException("Partner " + partnerId + " has no campsites to import records from");
        }

        List<Long> groupIds = groupService.findManageable(partner.getSiteGroup());
        final Set<Long> manageableGroups = new HashSet<>(groupIds);

        return acceptImportedRecords(records, manageableGroups, sourceName);
    }

    List<ConsumptionRecordDTO> acceptImportedRecords(List<ConsumptionRecordDTO> records, Set<Long> manageableGroups,
                                                     String sourceName) {
        List<ConsumptionRecordDTO> acceptedRecords = new ArrayList<>();
        List<ConsumptionRecord> savedRecords = new ArrayList<>();

        LOGGER.info(String.format("Consumption records to import from %s: %s", sourceName, records.size()));

        for (ConsumptionRecordDTO line : records) {
            ConsumptionRecord record = createConsumptionRecord(line, manageableGroups, sourceName);
            if (record != null) {
                acceptedRecords.add(line);
                if (record.getId() == null) {
                    savedRecords.add(consumptionRecordDao.save(record));
                }
            }
        }

        LOGGER.info(String.format("Saved consumption records: %s, accepted lines: %s", savedRecords.size(),
                acceptedRecords.size()));
        return acceptedRecords;
    }

    /**
     * The record of one line, not saved yet; the existing record when the line was imported before; null when the
     * line is refused (logged).
     */
    ConsumptionRecord createConsumptionRecord(ConsumptionRecordDTO line, Set<Long> manageableGroups,
                                              String sourceName) {
        if (isEmpty(line.recordNo())) {
            LOGGER.error(String.format("Record number is missing in the line for card %s", line.cardRfid()));
            return null;
        }

        if (isEmpty(line.cardRfid())) {
            LOGGER.error(String.format("Card is missing in record %s", line.recordNo()));
            return null;
        }

        if (isEmpty(line.socketCode())) {
            LOGGER.error(String.format("Socket code is missing in record %s (card %s)", line.recordNo(),
                    line.cardRfid()));
            return null;
        }

        if (line.plugOut() == null) {
            LOGGER.error(String.format("Plug-out is missing in record %s", line.recordNo()));
            return null;
        }

        if (isEmpty(sourceName)) {
            LOGGER.error("Source name must be set");
            return null;
        }

        // imported before: the line counts as accepted again
        ConsumptionRecord alreadyImported = consumptionRecordDao.findBySourceNameAndExternalRecordNo(sourceName,
                line.recordNo());
        if (alreadyImported != null) {
            LOGGER.info(String.format("Record already imported (card %s, record %s)", line.cardRfid(),
                    line.recordNo()));
            return alreadyImported;
        }

        final CampingCard card = campingCardDao.findByCardRfid(line.cardRfid());
        if (card == null || card.getGuest() == null) {
            LOGGER.warn(String.format("No card found for record %s (card %s)", line.recordNo(), line.cardRfid()));
            return null;
        }

        final Socket socket = socketDao.findBySocketCode(line.socketCode());
        if (socket == null || socket.getPedestal() == null || socket.getPedestal().getGroup() == null
                || !manageableGroups.contains(socket.getPedestal().getGroup().getId())) {
            LOGGER.warn(String.format("Socket %s of record %s is not a socket of %s", line.socketCode(),
                    line.recordNo(), sourceName));
            return null;
        }

        ConsumptionRecord record = new ConsumptionRecord();
        record.setSourceName(sourceName);
        record.setExternalRecordNo(line.recordNo());
        record.setCreateDate(dateService.getNow());

        Pedestal pedestal = socket.getPedestal();
        record.setSocketId(socket.getId());
        record.setSocketCode(socket.getSocketCode());
        record.setSocketName(socket.getName());
        record.setPitchNo(line.pitchNo() != null ? line.pitchNo() : socket.getPitchNo());
        record.setPedestalId(pedestal.getId());
        record.setPedestalCode(pedestal.getCode());
        record.setPedestalReferenceId(pedestal.getReferenceId());
        record.setPedestalType(pedestal.getPedestalType() != null ? pedestal.getPedestalType().getName() : null);
        record.setSiteGroupId(pedestal.getGroup().getId());
        record.setSiteTenant(siteTenantService.findSiteTenantNameFor(pedestal));
        Location location = pedestal.getLocation();
        if (location != null) {
            record.setCampsiteCode(location.getCampsiteCode());
            record.setCampsiteName(location.getName());
            record.setCity(location.getCity());
            record.setZipcode(location.getZipcode());
            record.setStreet(location.getStreet());
            record.setHouseNumber(location.getHouseNumber());
            record.setCountryIso(location.getCountryIso());
        } else {
            record.setCampsiteName(line.campsiteName());
        }

        Guest guest = card.getGuest();
        Group issuerGroup = guest.getGroup();
        record.setCardId(card.getId());
        record.setCardRfid(card.getCardRfid());
        record.setCardNumber(card.getCardNumber());
        record.setBillingAccountNo(card.getBillingAccountNo());
        record.setGuestId(guest.getId());
        record.setGuestName(guest.getFullName());
        record.setErpPartnerNo(guest.getErpPartnerNo());
        record.setIssuerGroupId(issuerGroup != null ? issuerGroup.getId() : null);
        record.setIssuerTenant(issuerTenantService.findIssuerTenantNameFor(issuerGroup));
        record.setPaymentChannel(PaymentChannel.CARD);
        record.setSettlementFlags(SettlementFlags.forCampingCard(true, pedestal.isSettledByCampsite()));

        record.setKwh(line.kwh());
        record.setPlugIn(line.plugIn());
        record.setPlugOut(line.plugOut());
        record.setDurationSeconds(line.durationSeconds() != null ? line.durationSeconds()
                : line.plugIn() != null ? Duration.between(line.plugIn(), line.plugOut()).getSeconds() : null);
        record.setNights(line.plugIn() != null ? PricingInput.nightsBetween(line.plugIn(), line.plugOut())
                : Math.max(1, line.nights()));

        boolean suspect = consumptionRecordFactory.completeNewRecord(record);
        if (!suspect) {
            applyPartnerPrice(record, line);
        }
        return record;
    }

    /**
     * The partner priced the supply at its campsite; without a cost the record waits for a review.
     */
    static void applyPartnerPrice(ConsumptionRecord record, ConsumptionRecordDTO line) {
        if (line.cost() == null || isEmpty(line.currency())) {
            record.setPricingStatus(PricingStatus.PRICING_FAILED);
            record.setPricingFailureReason(IMPORTED_WITHOUT_COST);
            record.setExportStatus(ExportStatus.REVIEW);
            return;
        }
        record.setPricingStatus(PricingStatus.PRICED);
        record.setCost(line.cost().setScale(2, RoundingMode.HALF_UP));
        record.setCurrency(line.currency());
        record.setTariffName(line.tariffName());
        record.setExportStatus(ExportStatus.NEW);
    }

    private static boolean isEmpty(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
