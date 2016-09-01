package io.pillopl.consumption;

import io.pillopl.card.CampingCard;
import io.pillopl.card.HouseCard;
import io.pillopl.card.ValidationService.ValidationResult;
import io.pillopl.group.Group;
import io.pillopl.group.attribute.IssuerTenantService;
import io.pillopl.group.attribute.SiteTenantService;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.Location;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.pricing.PriceCalculation;
import io.pillopl.pricing.PricingInput;
import io.pillopl.pricing.PricingService;
import io.pillopl.supply.Consumption;
import io.pillopl.supply.StopReason;
import io.pillopl.supply.SupplyDuration;
import io.pillopl.supply.SupplySession;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Writes the one consumption record of a finished supply session (stop of the pedestal or the job closing stale
 * sessions) and handles its review: snapshot of pedestal, address, card, guest and tenants, settlement flags,
 * suspect rule, price, save, and the confirmation or rejection of records waiting in the review queue.
 */
@Service
@Transactional
public class ConsumptionRecordWriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordWriteService.class);

    static final String N_A = "N/A";

    private final ConsumptionRecordDao consumptionRecordDao;

    private final ConsumptionRecordFactory consumptionRecordFactory;

    private final PricingService pricingService;

    private final SiteTenantService siteTenantService;

    private final IssuerTenantService issuerTenantService;

    private final DateService dateService;

    private final ObjectProvider<SuspectRecordNotifier> suspectRecordNotifier;

    public ConsumptionRecordWriteService(ConsumptionRecordDao consumptionRecordDao,
                                         ConsumptionRecordFactory consumptionRecordFactory,
                                         PricingService pricingService, SiteTenantService siteTenantService,
                                         IssuerTenantService issuerTenantService, DateService dateService,
                                         ObjectProvider<SuspectRecordNotifier> suspectRecordNotifier) {
        this.consumptionRecordDao = consumptionRecordDao;
        this.consumptionRecordFactory = consumptionRecordFactory;
        this.pricingService = pricingService;
        this.siteTenantService = siteTenantService;
        this.issuerTenantService = issuerTenantService;
        this.dateService = dateService;
        this.suspectRecordNotifier = suspectRecordNotifier;
    }

    /**
     * Creates the consumption record of a finished session. A session has one record: a second call for the same
     * session returns the existing record.
     *
     * @param billed whether the supply is billed (house card, pedestal not billable, free socket: false)
     */
    public ConsumptionRecord createConsumptionRecord(SupplySession session, ValidationResult validationResult,
                                                     Consumption consumption, SupplyDuration duration,
                                                     boolean billed, StopReason stopReason,
                                                     LocalDateTime systemDate) {
        ConsumptionRecord existing = consumptionRecordDao.findBySupplySessionId(session.getId());
        if (existing != null) {
            LOGGER.error(String.format("Session %s has its consumption record %s already, no second record",
                    session, existing.getRecordNo()));
            return existing;
        }

        ConsumptionRecord record = new ConsumptionRecord();
        record.setSupplySessionId(session.getId());
        record.setTransactionId(session.getTransactionId());
        record.setStayId(session.getStayId());
        record.setUnitPlate(session.getUnitPlate());
        record.setMaxCurrentA(session.getMaxCurrentA());
        record.setStopReason(stopReason);
        record.setCreateDate(systemDate);

        // pedestal, socket and campsite as they are now
        Socket socket = validationResult.getSocket() != null ? validationResult.getSocket() : session.getSocket();
        Pedestal pedestal = socket.getPedestal();
        record.setSocketId(socket.getId());
        record.setSocketCode(socket.getSocketCode());
        record.setSocketName(socket.getName());
        record.setPitchNo(socket.getPitchNo());
        record.setPedestalId(pedestal.getId());
        record.setPedestalCode(pedestal.getCode());
        record.setPedestalReferenceId(pedestal.getReferenceId() == null ? N_A : pedestal.getReferenceId());
        record.setPedestalType(pedestal.getPedestalType() != null ? pedestal.getPedestalType().getName() : N_A);
        record.setSiteGroupId(pedestal.getGroup() != null ? pedestal.getGroup().getId() : null);
        record.setSiteTenant(siteTenantService.findSiteTenantNameFor(pedestal));

        Location location = pedestal.getLocation();
        record.setCampsiteCode(location != null ? location.getCampsiteCode() : null);
        record.setCampsiteName(orNa(location, () -> location.getName()));
        record.setCity(orNa(location, () -> location.getCity()));
        record.setZipcode(orNa(location, () -> location.getZipcode()));
        record.setStreet(orNa(location, () -> location.getStreet()));
        record.setHouseNumber(orNa(location, () -> location.getHouseNumber()));
        record.setCountryIso(orNa(location, () -> location.getCountryIso()));

        // house card (the campsite's own use) or the guest's camping card
        HouseCard houseCard = validationResult.getHouseCard() != null ? validationResult.getHouseCard()
                : session.getHouseCard();
        CampingCard card = validationResult.getCard() != null ? validationResult.getCard() : session.getCard();
        if (houseCard != null) {
            record.setHouseCardId(houseCard.getId());
            record.setCardRfid(houseCard.getRfid());
            record.setCardNumber(houseCard.getCardNumber());
            record.setHouseCardType(houseCard.getHouseCardType() != null ? houseCard.getHouseCardType().name()
                    : null);
            record.setSettlementFlags(SettlementFlags.forHouseCard(houseCard.isRental()));
        } else if (card != null) {
            record.setCardId(card.getId());
            record.setCardRfid(card.getCardRfid());
            record.setCardNumber(card.getCardNumber());
            record.setBillingAccountNo(card.getBillingAccountNo());
            Guest guest = validationResult.getGuest() != null ? validationResult.getGuest() : card.getGuest();
            if (guest != null) {
                Group issuerGroup = guest.getGroup();
                record.setGuestId(guest.getId());
                record.setGuestName(guest.getFullName());
                record.setErpPartnerNo(guest.getErpPartnerNo());
                record.setIssuerGroupId(issuerGroup != null ? issuerGroup.getId() : null);
                record.setIssuerTenant(issuerTenantService.findIssuerTenantNameFor(issuerGroup));
            }
            record.setSettlementFlags(SettlementFlags.forCampingCard(billed, pedestal.isSettledByCampsite()));
        } else {
            record.setSettlementFlags(SettlementFlags.forCampingCard(billed, pedestal.isSettledByCampsite()));
        }
        record.setPaymentChannel(PaymentChannel.of(houseCard != null, card != null));

        // supply
        record.setStartMeterKwh(consumption.startMeterKwh());
        record.setStopMeterKwh(consumption.stopMeterKwh());
        record.setKwh(consumption.consumptionKwh());
        record.setPlugIn(duration.plugIn());
        record.setPlugOut(duration.plugOut());
        record.setDurationSeconds(duration.durationInSeconds());
        record.setNights(PricingInput.nightsBetween(duration.plugIn(), duration.plugOut()));

        boolean suspect = consumptionRecordFactory.completeNewRecord(record);
        if (!suspect) {
            // suspect records are priced when someone confirms them
            record.applyPrice(price(record));
        }

        ConsumptionRecord saved = consumptionRecordDao.save(record);
        if (suspect) {
            notifySuspectRecord(saved);
        }
        return saved;
    }

    /**
     * Confirms a record of the review queue: a suspect record becomes CONFIRMED, and the record is priced again
     * (it is exported when the price is known, else it stays in review). An imported record keeps its partner's
     * price and is never priced with the network tariff.
     *
     * @return the record or null when there is none with this id
     * @throws IllegalStateException when the record does not wait for a review
     */
    public ConsumptionRecord confirmRecord(Long recordId, String reviewer, String comment) {
        ConsumptionRecord record = recordInReview(recordId);
        if (record == null) {
            return null;
        }
        if (record.isSuspect()) {
            record.confirm(reviewer, dateService.getNow(), comment);
        }
        if (record.isImported()) {
            record.applyPartnerPrice();
        } else {
            record.applyPrice(price(record));
        }
        LOGGER.info(String.format("Consumption record %s confirmed by %s: %s", record.getRecordNo(), reviewer,
                record));
        return record;
    }

    /**
     * Rejects a record of the review queue: it is never exported.
     *
     * @return the record or null when there is none with this id
     * @throws IllegalStateException when the record does not wait for a review
     */
    public ConsumptionRecord rejectRecord(Long recordId, String reviewer, String comment) {
        ConsumptionRecord record = recordInReview(recordId);
        if (record == null) {
            return null;
        }
        record.reject(reviewer, dateService.getNow(), comment);
        LOGGER.info(String.format("Consumption record %s rejected by %s", record.getRecordNo(), reviewer));
        return record;
    }

    /**
     * Prices a record again with the network tariff, e.g. after a tariff was fixed. Only records whose price may
     * still change: pricing failed (REVIEW) or waiting for the export (NEW). A suspect record is reviewed first, a
     * rejected or exported one keeps its price, an imported one keeps its partner's price.
     *
     * @return the record or null when there is none with this id
     * @throws IllegalStateException when the record must not be priced again
     */
    public ConsumptionRecord repriceRecord(Long recordId, String by) {
        ConsumptionRecord record = consumptionRecordDao.findById(recordId).orElse(null);
        if (record == null) {
            return null;
        }
        if (record.isImported()) {
            throw new IllegalStateException("Consumption record " + record.getRecordNo()
                    + " is imported and keeps the price of its partner");
        }
        if (record.isSuspect()) {
            throw new IllegalStateException("Consumption record " + record.getRecordNo()
                    + " is suspect: confirm or reject it first");
        }
        ExportStatus status = record.getExportStatus();
        if (status != ExportStatus.REVIEW && status != ExportStatus.NEW) {
            throw new IllegalStateException("Consumption record " + record.getRecordNo()
                    + " cannot be priced again (" + status + ")");
        }
        record.applyPrice(price(record));
        LOGGER.info(String.format("Consumption record %s priced again by %s: %s", record.getRecordNo(), by, record));
        return record;
    }

    /**
     * A record whose export failed (ERROR) waits for the next export again (NEW).
     *
     * @return the record or null when there is none with this id
     * @throws IllegalStateException when the export of the record did not fail
     */
    public ConsumptionRecord requeueExport(Long recordId, String by) {
        ConsumptionRecord record = consumptionRecordDao.findById(recordId).orElse(null);
        if (record == null) {
            return null;
        }
        if (record.getExportStatus() != ExportStatus.ERROR) {
            throw new IllegalStateException("Export of consumption record " + record.getRecordNo()
                    + " did not fail (" + record.getExportStatus() + ")");
        }
        record.setExportStatus(ExportStatus.NEW);
        LOGGER.info(String.format("Export of consumption record %s queued again by %s", record.getRecordNo(), by));
        return record;
    }

    private ConsumptionRecord recordInReview(Long recordId) {
        ConsumptionRecord record = consumptionRecordDao.findById(recordId).orElse(null);
        if (record != null && record.getExportStatus() != ExportStatus.REVIEW) {
            throw new IllegalStateException("Consumption record " + record.getRecordNo()
                    + " does not wait for a review (" + record.getExportStatus() + ")");
        }
        return record;
    }

    /**
     * Price of the record from the one tariff model. Not billed: NOT_BILLABLE with cost 0; billed with an unknown
     * consumption: PRICING_FAILED (the meter readings were unusable).
     */
    PriceCalculation price(ConsumptionRecord record) {
        if (!record.getSettlementFlags().isBillable()) {
            return pricingService.calculateCost(PricingInput.notBillable(record.getSocketId(), record.getCardId(),
                    record.getPlugIn(), record.getPlugOut(), record.getKwh()));
        }
        if (record.getKwh() == null) {
            LOGGER.warn("Consumption unknown, record cannot be priced: " + record);
            return PriceCalculation.failed(null, "Consumption unknown: unusable meter reading");
        }
        return pricingService.calculateCost(PricingInput.of(record.getSocketId(), record.getCardId(),
                record.getPlugIn(), record.getPlugOut(), record.getKwh(), record.getNights()));
    }

    private void notifySuspectRecord(ConsumptionRecord record) {
        SuspectRecordNotifier notifier = suspectRecordNotifier.getIfAvailable();
        if (notifier == null) {
            LOGGER.warn(String.format("Suspect consumption record %s waits for review (no notifier): %s",
                    record.getRecordNo(), record.getSuspectReason()));
            return;
        }
        try {
            notifier.suspectRecordCreated(record);
        } catch (RuntimeException e) {
            LOGGER.error("Could not notify the suspect consumption record " + record.getRecordNo(), e);
        }
    }

    private static String orNa(Location location, Supplier<String> value) {
        if (location == null) {
            return N_A;
        }
        String v = value.get();
        return v != null ? v : N_A;
    }
}
