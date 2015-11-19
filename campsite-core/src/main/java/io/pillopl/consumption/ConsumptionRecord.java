package io.pillopl.consumption;

import io.pillopl.pricing.PriceCalculation;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.supply.StopReason;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The one settlement record of a supply session. A denormalised snapshot taken when the session ends: pedestal,
 * socket and pitch, campsite address, card, guest and billing account, operator and issuer tenants, settlement
 * flags, meter values, duration, nights and price. Suspect records stay here with {@link RecordValidationState#SUSPECT}
 * and {@link ExportStatus#REVIEW} until they are reviewed.
 */
@Entity
@Table(name = "CONSUMPTION_RECORD")
@Getter
@Setter
public class ConsumptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_consumption_record")
    @SequenceGenerator(name = "seq_consumption_record", sequenceName = "CONSUMPTION_RECORD_ID_SEQ",
            allocationSize = 1)
    private Long id;

    /**
     * Natural id {@code <campsite code>-<yyyyMMdd>-<number>}.
     */
    @Column(name = "RECORD_NO", length = 64, nullable = false, unique = true)
    private String recordNo;

    /**
     * The network's supply session; null for a record imported from an issuer partner.
     */
    @Column(name = "SUPPLY_SESSION_ID", unique = true)
    private Long supplySessionId;

    /**
     * Name of the issuer partner an imported record comes from, null for the network's own records.
     */
    @Column(name = "SOURCE_NAME", length = 100)
    private String sourceName;

    /**
     * The partner's number of an imported record.
     */
    @Column(name = "EXTERNAL_RECORD_NO", length = 64)
    private String externalRecordNo;

    @Column(name = "TRANSACTION_ID")
    private Integer transactionId;

    // ------------------------------------------------------------------ infrastructure

    @Column(name = "SOCKET_ID")
    private Long socketId;

    @Column(name = "SOCKET_CODE", length = 64)
    private String socketCode;

    @Column(name = "SOCKET_NAME", length = 128)
    private String socketName;

    @Column(name = "PITCH_NO", length = 16)
    private String pitchNo;

    @Column(name = "PEDESTAL_ID")
    private Long pedestalId;

    @Column(name = "PEDESTAL_CODE", length = 128)
    private String pedestalCode;

    @Column(name = "PEDESTAL_REFERENCE_ID", length = 50)
    private String pedestalReferenceId;

    @Column(name = "PEDESTAL_TYPE", length = 128)
    private String pedestalType;

    /**
     * SITE group of the pedestal when the supply ended: the operator side of the visibility.
     */
    @Column(name = "SITE_GROUP_ID")
    private Long siteGroupId;

    @Column(name = "SITE_TENANT", length = 255)
    private String siteTenant;

    // ------------------------------------------------------------------ campsite address

    @Column(name = "CAMPSITE_CODE", length = 32)
    private String campsiteCode;

    @Column(name = "CAMPSITE_NAME", length = 256)
    private String campsiteName;

    @Column(name = "CITY", length = 128)
    private String city;

    @Column(name = "ZIPCODE", length = 32)
    private String zipcode;

    @Column(name = "STREET", length = 255)
    private String street;

    @Column(name = "HOUSE_NUMBER", length = 32)
    private String houseNumber;

    @Column(name = "COUNTRY_ISO", length = 8)
    private String countryIso;

    // ------------------------------------------------------------------ card and guest

    @Column(name = "CARD_ID")
    private Long cardId;

    @Column(name = "CARD_RFID", length = 64)
    private String cardRfid;

    @Column(name = "CARD_NUMBER", length = 64)
    private String cardNumber;

    @Column(name = "HOUSECARD_ID")
    private Long houseCardId;

    @Column(name = "HOUSE_CARD_TYPE", length = 16)
    private String houseCardType;

    @Column(name = "GUEST_ID")
    private Long guestId;

    @Column(name = "GUEST_NAME", length = 255)
    private String guestName;

    @Column(name = "BILLING_ACCOUNT_NO", length = 64)
    private String billingAccountNo;

    @Column(name = "ERP_PARTNER_NO", length = 64)
    private String erpPartnerNo;

    /**
     * ISSUER group of the guest when the supply ended: the issuer side of the visibility (null for house cards).
     */
    @Column(name = "ISSUER_GROUP_ID")
    private Long issuerGroupId;

    @Column(name = "ISSUER_TENANT", length = 255)
    private String issuerTenant;

    @Column(name = "STAY_ID")
    private Long stayId;

    @Column(name = "UNIT_PLATE", length = 32)
    private String unitPlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_CHANNEL", length = 16, nullable = false)
    private PaymentChannel paymentChannel;

    @Embedded
    private SettlementFlags settlementFlags = new SettlementFlags(false, false, false);

    // ------------------------------------------------------------------ supply

    @Column(name = "START_METER_KWH", precision = 19, scale = 4)
    private BigDecimal startMeterKwh;

    @Column(name = "STOP_METER_KWH", precision = 19, scale = 4)
    private BigDecimal stopMeterKwh;

    /**
     * Consumption in kWh, null when a meter reading of the pedestal was unusable.
     */
    @Column(name = "KWH", precision = 19, scale = 4)
    private BigDecimal kwh;

    @Column(name = "PLUG_IN")
    private LocalDateTime plugIn;

    @Column(name = "PLUG_OUT", nullable = false)
    private LocalDateTime plugOut;

    @Column(name = "DURATION_S")
    private Long durationSeconds;

    @Column(name = "NIGHTS", nullable = false)
    private int nights;

    @Column(name = "MAX_CURRENT_A")
    private Integer maxCurrentA;

    @Enumerated(EnumType.STRING)
    @Column(name = "STOP_REASON", length = 32)
    private StopReason stopReason;

    // ------------------------------------------------------------------ price

    /**
     * Cost in the tariff's currency, scale 2; null while not priced or when the pricing failed.
     */
    @Column(name = "COST", precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "CURRENCY", length = 3)
    private String currency;

    /**
     * Null while a suspect record waits for its review (it is priced when confirmed).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "PRICING_STATUS", length = 16)
    private PricingStatus pricingStatus;

    @Column(name = "PRICING_FAILURE_REASON", length = 500)
    private String pricingFailureReason;

    @Column(name = "TARIFF_ID")
    private Long tariffId;

    @Column(name = "TARIFF_NAME", length = 255)
    private String tariffName;

    // ------------------------------------------------------------------ validation and export

    @Enumerated(EnumType.STRING)
    @Column(name = "VALIDATION_STATE", length = 16, nullable = false)
    private RecordValidationState validationState = RecordValidationState.VALID;

    @Column(name = "SUSPECT_REASON", length = 255)
    private String suspectReason;

    @Column(name = "REVIEWED_BY", length = 255)
    private String reviewedBy;

    @Column(name = "REVIEW_DATE")
    private LocalDateTime reviewDate;

    @Column(name = "REVIEW_COMMENT", length = 1000)
    private String reviewComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "EXPORT_STATUS", length = 16, nullable = false)
    private ExportStatus exportStatus = ExportStatus.NEW;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate;

    public ConsumptionRecord() {
    }

    public boolean isSuspect() {
        return validationState == RecordValidationState.SUSPECT;
    }

    public boolean isImported() {
        return sourceName != null;
    }

    public boolean isForHouseCard() {
        return houseCardId != null;
    }

    /**
     * Takes the result of the pricing. A failed pricing leaves the cost empty and puts the record into the review
     * queue; otherwise the record is exported.
     */
    public void applyPrice(PriceCalculation calculation) {
        this.pricingStatus = calculation.getStatus();
        this.cost = calculation.getCost();
        this.currency = calculation.getCurrency();
        this.tariffId = calculation.getTariffId();
        this.tariffName = calculation.getTariffName();
        this.pricingFailureReason = calculation.getFailureReason();
        this.exportStatus = pricingStatus == PricingStatus.PRICING_FAILED ? ExportStatus.REVIEW : ExportStatus.NEW;
    }

    /**
     * The record's values are implausible: it waits for a review, not priced and not exported.
     */
    public void markSuspect(String reason) {
        this.validationState = RecordValidationState.SUSPECT;
        this.suspectReason = reason;
        this.exportStatus = ExportStatus.REVIEW;
    }

    public void confirm(String reviewer, LocalDateTime at, String comment) {
        this.validationState = RecordValidationState.CONFIRMED;
        reviewed(reviewer, at, comment);
    }

    /**
     * A rejected record is never exported.
     */
    public void reject(String reviewer, LocalDateTime at, String comment) {
        this.validationState = RecordValidationState.REJECTED;
        this.exportStatus = ExportStatus.IGNORED;
        reviewed(reviewer, at, comment);
    }

    private void reviewed(String reviewer, LocalDateTime at, String comment) {
        this.reviewedBy = reviewer;
        this.reviewDate = at;
        this.reviewComment = comment;
    }

    @Override
    public String toString() {
        return "ConsumptionRecord{id=" + id + ", recordNo=" + recordNo + ", session=" + supplySessionId + ", socket="
                + socketCode + ", card=" + cardRfid + ", kwh=" + kwh + ", plugIn=" + plugIn + ", plugOut=" + plugOut
                + ", nights=" + nights + ", cost=" + cost + " " + currency + ", pricing=" + pricingStatus
                + ", validation=" + validationState + ", export=" + exportStatus + ", " + settlementFlags + "}";
    }
}
