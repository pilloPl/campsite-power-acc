package io.pillopl.erp;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.erp.ErpBillingLine.LineType;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.guest.PackageItem;
import io.pillopl.guest.SeasonalContract;
import io.pillopl.guest.SeasonalContractService;
import io.pillopl.order.ProductDefinition;
import io.pillopl.persistence.dao.BillingAccountDao;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.ConsumptionRecordDaoCustom.ErpConsumption;
import io.pillopl.persistence.dao.ErpBillingLineDao;
import io.pillopl.pricing.PriceCalculation;
import io.pillopl.pricing.PricingInput;
import io.pillopl.pricing.PricingService;
import io.pillopl.util.DateService;
import io.pillopl.value.BillingPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Monthly billing of the network's billing accounts to the ERP. For every account: the seasonal contracts running
 * in the month bring their fixed fee (the share of the month) and the kWh above their package (priced by the
 * contract's tariff); the power of the other supplies (transit stays, other pitches) is billed at the cost of its
 * consumption records. The only source of consumption is CONSUMPTION_RECORD; prices come from the pricing module.
 */
@Service
@Transactional
public class ErpBillingDataService {

    private static final Logger LOG = LoggerFactory.getLogger(ErpBillingDataService.class);

    /**
     * How far back not billed records are still billed (records confirmed late after a review).
     */
    static final int CONSUMPTION_MONTHS_BACK = 12;

    static final int FRACTION_SCALE = 6;

    static final String DEFAULT_CURRENCY = "EUR";

    private final SystemPropertyService systemPropertyService;

    private final DateService dateService;

    private final BillingAccountDao billingAccountDao;

    private final SeasonalContractService seasonalContractService;

    private final ConsumptionRecordDao consumptionRecordDao;

    private final ErpBillingLineDao erpBillingLineDao;

    private final PricingService pricingService;

    public ErpBillingDataService(SystemPropertyService systemPropertyService, DateService dateService,
                                 BillingAccountDao billingAccountDao, SeasonalContractService seasonalContractService,
                                 ConsumptionRecordDao consumptionRecordDao, ErpBillingLineDao erpBillingLineDao,
                                 PricingService pricingService) {
        this.systemPropertyService = systemPropertyService;
        this.dateService = dateService;
        this.billingAccountDao = billingAccountDao;
        this.seasonalContractService = seasonalContractService;
        this.consumptionRecordDao = consumptionRecordDao;
        this.erpBillingLineDao = erpBillingLineDao;
        this.pricingService = pricingService;
    }

    /**
     * Calculates the ERP lines of the month without saving them (the run, the preview of the console).
     */
    @Transactional(readOnly = true)
    public List<ErpBillingLine> findAndCalculateErpBillingLines(int month, int year) {

        // constant data from SYSTEM_PROPERTY
        String c_articleTaxClass = systemPropertyService.getErpArticleTaxClass();
        String c_articleClassification = systemPropertyService.getErpArticleClassification();
        String c_salesOffice = systemPropertyService.getErpSalesOffice();
        String c_salesGroup = systemPropertyService.getErpSalesGroup();
        String c_assignee = systemPropertyService.getErpAssignee();
        String c_orderType = systemPropertyService.getErpOrderType();
        String c_alternateTextOrganisation = systemPropertyService.getErpAlternateTextOrganisation();
        String c_alternateTextPrivate = systemPropertyService.getErpAlternateTextPrivate();
        String c_powerArticleNo = systemPropertyService.getErpPowerArticleNo();
        BigDecimal c_minimumBillAmount = systemPropertyService.getErpMinimumBillAmount();

        BillingPeriod period = BillingPeriod.of(month, year);
        LocalDate periodFrom = period.getFirstDay();
        LocalDate periodTo = period.getLastDay();
        LocalDateTime consumptionFrom = periodFrom.minusMonths(CONSUMPTION_MONTHS_BACK).atStartOfDay();
        LocalDateTime consumptionTo = periodTo.plusDays(1).atStartOfDay();
        LocalDateTime now = dateService.getNow();

        // the consumption of every account, read once
        Map<String, List<ErpConsumption>> consumptionPerAccount = new HashMap<>();
        for (ErpConsumption consumption : consumptionRecordDao.findErpConsumptionPerMonth(consumptionFrom,
                consumptionTo)) {
            consumptionPerAccount.computeIfAbsent(consumption.billingAccountNo(), no -> new ArrayList<>())
                    .add(consumption);
        }

        // define result
        List<ErpBillingLine> erpBillingLines = new ArrayList<>();

        // Each billing account becomes one bill: its lines per ERP article.
        for (BillingAccount account : billingAccountDao.findByErpAccountNoIsNotNullOrderById()) {
            if (account.isClosed() && account.getCloseDate() != null
                    && account.getCloseDate().toLocalDate().isBefore(periodFrom)) {
                continue;
            }
            List<ErpConsumption> accountConsumption = consumptionPerAccount.getOrDefault(account.getErpAccountNo(),
                    List.of());
            SortedMap<String, ErpBillingLine> linesPerArticleNo = new TreeMap<>();
            Set<Long> contractTariffIds = new HashSet<>();
            String orderNumber = null;
            boolean accountFailed = false;

            // Each contract running in the period holds a product that should be invoiced.
            List<SeasonalContract> contracts = seasonalContractService.findContractsActiveIn(account.getId(),
                    periodFrom, periodTo);
            for (SeasonalContract contract : contracts) {
                contractTariffIds.add(contract.getTariff().getId());
                ProductDefinition product = getContractProduct(contract);
                if (product == null) {
                    LOG.error(String.format("Contract %s of account %s has no product, account not billed in %s",
                            contract.getId(), account.getErpAccountNo(), period));
                    accountFailed = true;
                    break;
                }
                if (orderNumber == null && contract.getOrderItem() != null) {
                    orderNumber = contract.getOrderItem().getOrder().getOrderNumber();
                }
                LocalDate lastDay = contract.getLastDay();

                // A contract has a fixed fee: its share of the period.
                if (contract.getFixedFee().signum() > 0 && product.getFixedPriceErpArticleNo() != null) {
                    BigDecimal fraction = getFeeFraction(contract, period);
                    BigDecimal feeAmount = contract.getFixedFee().multiply(fraction)
                            .setScale(PricingService.MONEY_SCALE, RoundingMode.HALF_UP);
                    if (feeAmount.signum() > 0) {
                        ErpBillingLine line = linesPerArticleNo.computeIfAbsent(product.getFixedPriceErpArticleNo(),
                                articleNo -> new ErpBillingLine(period.getCode(), account, LineType.FIXED_FEE,
                                        articleNo));
                        line.add(fraction.setScale(ErpBillingLine.UNIT_PRICE_SCALE, RoundingMode.HALF_UP), feeAmount);
                        line.setCurrency(product.getCurrency());
                        if (line.getDescription() == null) {
                            line.setDescription(String.format("Fixed fee %s %s pitch %s", contract.getFeePeriod()
                                    .name().toLowerCase(), period, contract.getPitchNo()));
                        }
                    }
                }

                // A contract has consumption: the kWh above the package are invoiced.
                String seasonStart = new BillingPeriod(YearMonth.from(contract.getStartDate())).getCode();
                BigDecimal billedKwh = BigDecimal.ZERO;
                BigDecimal newKwh = BigDecimal.ZERO;
                for (ErpConsumption consumption : accountConsumption) {
                    if (!contract.getTariff().getId().equals(consumption.tariffId())
                            || consumption.supplyPeriod().compareTo(seasonStart) < 0) {
                        continue;
                    }
                    if (!consumption.billed()) {
                        newKwh = newKwh.add(consumption.kwh());
                    } else if (contract.getFeePeriod() == SeasonalContract.FeePeriod.SEASONAL) {
                        // the package of a season is used up across its months; a monthly package starts anew
                        billedKwh = billedKwh.add(consumption.kwh());
                    }
                }
                BigDecimal packageKwh = contract.getPackageKwh();
                BigDecimal overageKwh = billedKwh.add(newKwh).subtract(packageKwh).max(BigDecimal.ZERO)
                        .subtract(billedKwh.subtract(packageKwh).max(BigDecimal.ZERO));
                if (overageKwh.signum() > 0 && product.getUnitPriceErpArticleNo() != null) {
                    LocalDateTime pricedAt = (lastDay != null && lastDay.isBefore(periodTo) ? lastDay : periodTo)
                            .atTime(12, 0);
                    PriceCalculation calculation = pricingService.calculateCost(contract.getTariff(),
                            PricingInput.of(null, null, pricedAt, pricedAt, overageKwh, 0));
                    if (!calculation.isPriced()) {
                        LOG.error(String.format("kWh above the package of contract %s not priced (%s), account %s"
                                + " not billed in %s", contract.getId(), calculation.getFailureReason(),
                                account.getErpAccountNo(), period));
                        accountFailed = true;
                        break;
                    }
                    ErpBillingLine line = linesPerArticleNo.computeIfAbsent(product.getUnitPriceErpArticleNo(),
                            articleNo -> new ErpBillingLine(period.getCode(), account, LineType.PACKAGE_OVERAGE,
                                    articleNo));
                    line.add(overageKwh, calculation.getCost());
                    line.setCurrency(calculation.getCurrency() != null ? calculation.getCurrency()
                            : product.getCurrency());
                    if (line.getDescription() == null) {
                        line.setDescription(String.format("kWh above the package of %s kWh, pitch %s",
                                packageKwh.stripTrailingZeros().toPlainString(), contract.getPitchNo()));
                    }
                }
            }
            if (accountFailed) {
                continue;
            }

            // The other supplies of the account (transit stays, other pitches) at the cost of their records.
            Set<String> powerPeriods = new LinkedHashSet<>();
            for (ErpConsumption consumption : accountConsumption) {
                if (consumption.billed() || contractTariffIds.contains(consumption.tariffId())
                        || consumption.cost() == null) {
                    continue;
                }
                ErpBillingLine line = linesPerArticleNo.computeIfAbsent(c_powerArticleNo,
                        articleNo -> new ErpBillingLine(period.getCode(), account, LineType.POWER, articleNo));
                line.add(consumption.kwh(), consumption.cost());
                powerPeriods.add(consumption.supplyPeriod());
                line.setDescription("Power " + String.join(", ", powerPeriods));
            }

            if (linesPerArticleNo.isEmpty()) {
                continue;
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (ErpBillingLine line : linesPerArticleNo.values()) {
                totalAmount = totalAmount.add(line.getAmount());
            }
            if (totalAmount.compareTo(c_minimumBillAmount) < 0) {
                LOG.warn(String.format("Bill of account %s in %s is %s, below the minimum of %s: not sent, the power"
                                + " waits for the next run", account.getErpAccountNo(), period, totalAmount,
                        c_minimumBillAmount));
                continue;
            }

            Guest guest = account.getGuest();
            String alternateText;
            if (guest.getOrganisationName() != null && !guest.getOrganisationName().isBlank()) {
                // organisation -> bill text for organisations
                alternateText = c_alternateTextOrganisation;
            } else {
                // private guest or nothing known -> bill text for private guests
                alternateText = c_alternateTextPrivate;
            }

            for (ErpBillingLine line : linesPerArticleNo.values()) {
                line.setErpPartnerNo(guest.getErpPartnerNo());
                line.setOrderNumber(orderNumber);
                line.setTaxClass(c_articleTaxClass);
                line.setArticleClassification(c_articleClassification);
                line.setSalesOffice(c_salesOffice);
                line.setSalesGroup(c_salesGroup);
                line.setAssignee(c_assignee);
                line.setOrderType(c_orderType);
                line.setAlternateText(alternateText);
                if (line.getCurrency() == null) {
                    line.setCurrency(DEFAULT_CURRENCY);
                }
                line.setCreateDate(now);
                erpBillingLines.add(line);
            }
        }

        return erpBillingLines;
    }

    /**
     * The billing run of a month: the lines are saved and the records they bill carry the period (WORK until the
     * ERP file is written). A month is billed once.
     */
    public List<ErpBillingLine> createErpBillingLines(int month, int year) {
        BillingPeriod period = BillingPeriod.of(month, year);
        if (erpBillingLineDao.existsByBillingPeriod(period.getCode())) {
            throw new IllegalStateException("Period " + period + " is billed already");
        }
        List<ErpBillingLine> lines = erpBillingLineDao.saveAll(findAndCalculateErpBillingLines(month, year));
        Set<String> billedAccountNos = new LinkedHashSet<>();
        for (ErpBillingLine line : lines) {
            billedAccountNos.add(line.getBillingAccountNo());
        }
        int records = consumptionRecordDao.markRecordsForErpBilling(billedAccountNos,
                period.getFirstDay().minusMonths(CONSUMPTION_MONTHS_BACK).atStartOfDay(),
                period.getLastDay().plusDays(1).atStartOfDay(), period.getCode());
        LOG.info(String.format("ERP billing %s: %s lines of %s accounts, %s consumption records", period,
                lines.size(), billedAccountNos.size(), records));
        return lines;
    }

    /**
     * The run of the month before today, from the configured day of the month on, unless done already.
     *
     * @return the lines created, empty before the day or when the month was billed before
     */
    public List<ErpBillingLine> createErpBillingLinesOfLastMonth() {
        LocalDate today = dateService.getToday();
        if (today.getDayOfMonth() < systemPropertyService.getErpBillingRunDayOfMonth()) {
            return List.of();
        }
        BillingPeriod period = BillingPeriod.before(today);
        if (erpBillingLineDao.existsByBillingPeriod(period.getCode())) {
            return List.of();
        }
        return createErpBillingLines(period.month().getMonthValue(), period.month().getYear());
    }

    @Transactional(readOnly = true)
    public List<ErpBillingLine> findErpBillingLines(String billingPeriod) {
        return erpBillingLineDao.findByBillingPeriodOrderByBillingAccountNoAscArticleNoAsc(
                BillingPeriod.parse(billingPeriod).getCode());
    }

    /**
     * Lines waiting for the next ERP file, oldest period first.
     */
    @Transactional(readOnly = true)
    public List<ErpBillingLine> findLinesToTransfer() {
        return erpBillingLineDao.findByExportStatusOrderByBillingPeriodAscBillingAccountNoAscArticleNoAsc(
                ExportStatus.NEW);
    }

    /**
     * The lines are in the ERP file; their records follow.
     */
    public void markLinesTransferred(Collection<ErpBillingLine> lines) {
        LocalDateTime now = dateService.getNow();
        Set<String> periodAndAccount = new LinkedHashSet<>();
        for (ErpBillingLine line : lines) {
            line.transferred(now);
            erpBillingLineDao.save(line);
            if (periodAndAccount.add(line.getBillingPeriod() + "/" + line.getBillingAccountNo())) {
                consumptionRecordDao.updateErpBilledRecords(line.getBillingPeriod(), line.getBillingAccountNo(),
                        List.of(ExportStatus.WORK), ExportStatus.TRANSFERRED);
            }
        }
    }

    /**
     * The ERP's answer to a line: booked or refused. The records of the account follow; one refused line of the
     * account puts its records in ERROR.
     *
     * @return false when the ERP answered a line that was never sent (logged)
     */
    public boolean takeErpAnswer(String billingPeriod, String billingAccountNo, String articleNo, boolean booked,
                                 String text) {
        String period = BillingPeriod.parse(billingPeriod).getCode();
        ErpBillingLine answered = null;
        for (ErpBillingLine line : erpBillingLineDao.findByBillingPeriodAndBillingAccountNoOrderByArticleNo(period,
                billingAccountNo)) {
            if (line.getArticleNo().equals(articleNo)) {
                answered = line;
            }
        }
        if (answered == null || answered.getExportStatus() == ExportStatus.NEW) {
            LOG.warn(String.format("ERP answer for line %s / %s / %s that was not sent", period, billingAccountNo,
                    articleNo));
            return false;
        }
        answered.answered(booked, text, dateService.getNow());
        erpBillingLineDao.save(answered);
        if (booked) {
            consumptionRecordDao.updateErpBilledRecords(period, billingAccountNo, List.of(ExportStatus.TRANSFERRED),
                    ExportStatus.PROCESSED);
        } else {
            LOG.warn(String.format("ERP refused line %s: %s", answered, text));
            consumptionRecordDao.updateErpBilledRecords(period, billingAccountNo,
                    List.of(ExportStatus.TRANSFERRED, ExportStatus.PROCESSED), ExportStatus.ERROR);
        }
        return true;
    }

    /**
     * The refused lines of a period go to the ERP again with the next file.
     *
     * @return number of lines to send again
     */
    public int resendRefusedLines(String billingPeriod) {
        String period = BillingPeriod.parse(billingPeriod).getCode();
        int resent = 0;
        for (ErpBillingLine line : erpBillingLineDao.findByBillingPeriodOrderByBillingAccountNoAscArticleNoAsc(
                period)) {
            if (line.getExportStatus() == ExportStatus.ERROR) {
                line.resend();
                erpBillingLineDao.save(line);
                resent++;
            }
        }
        if (resent > 0) {
            consumptionRecordDao.updateErpBilledRecords(period, null, List.of(ExportStatus.ERROR), ExportStatus.WORK);
        }
        return resent;
    }

    /**
     * Share of the period's days covered by [startDate, endDate] (both inclusive, a missing end is open).
     */
    public static BigDecimal getDateRangeFraction(LocalDate startDate, LocalDate endDate, BillingPeriod period) {
        LocalDate first = startDate != null && startDate.isAfter(period.getFirstDay()) ? startDate
                : period.getFirstDay();
        LocalDate last = endDate != null && endDate.isBefore(period.getLastDay()) ? endDate : period.getLastDay();
        if (last.isBefore(first)) {
            return BigDecimal.ZERO;
        }
        long daysIncluded = ChronoUnit.DAYS.between(first, last) + 1;
        if (daysIncluded == period.getDays()) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(daysIncluded).divide(BigDecimal.valueOf(period.getDays()), FRACTION_SCALE,
                RoundingMode.HALF_UP);
    }

    /**
     * Share of the contract's fixed fee billed in the period: of the month for a monthly fee; of the days of the
     * season for a seasonal fee (a termination ends it earlier), a season without end pays all with its first month.
     */
    static BigDecimal getFeeFraction(SeasonalContract contract, BillingPeriod period) {
        LocalDate lastDay = contract.getLastDay();
        if (contract.getFeePeriod() == SeasonalContract.FeePeriod.MONTHLY) {
            return getDateRangeFraction(contract.getStartDate(), lastDay, period);
        }
        if (contract.getEndDate() == null) {
            return YearMonth.from(contract.getStartDate()).equals(period.month()) ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        long seasonDays = ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate()) + 1;
        LocalDate first = contract.getStartDate().isAfter(period.getFirstDay()) ? contract.getStartDate()
                : period.getFirstDay();
        LocalDate last = lastDay.isBefore(period.getLastDay()) ? lastDay : period.getLastDay();
        long daysInPeriod = last.isBefore(first) ? 0 : ChronoUnit.DAYS.between(first, last) + 1;
        return BigDecimal.valueOf(daysInPeriod).divide(BigDecimal.valueOf(seasonDays), FRACTION_SCALE,
                RoundingMode.HALF_UP);
    }

    /**
     * The product of a contract: the one ordered, else the product of its first package item.
     */
    static ProductDefinition getContractProduct(SeasonalContract contract) {
        if (contract.getOrderItem() != null) {
            return contract.getOrderItem().getProductDefinition();
        }
        for (PackageItem item : contract.getPackageItems()) {
            if (item.getProductDefinition() != null) {
                return item.getProductDefinition();
            }
        }
        return null;
    }
}
