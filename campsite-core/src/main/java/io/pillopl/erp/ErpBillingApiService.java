package io.pillopl.erp;

import io.pillopl.api.ErpBillingApi;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ConsumptionRecordApiService;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.dto.erp.ErpBillingLineDTO;
import io.pillopl.dto.erp.ErpBillingRunDTO;
import io.pillopl.persistence.dao.ErpBillingLineDao;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.value.BillingPeriod;
import io.pillopl.value.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP billing calls of the console on top of {@link ErpBillingDataService}.
 */
@Service
public class ErpBillingApiService implements ErpBillingApi {

    private static final Logger LOG = LoggerFactory.getLogger(ErpBillingApiService.class);

    private final ErpBillingDataService erpBillingDataService;

    private final ErpBillingLineDao erpBillingLineDao;

    private final PrincipalResolver principalResolver;

    public ErpBillingApiService(ErpBillingDataService erpBillingDataService, ErpBillingLineDao erpBillingLineDao,
                                PrincipalResolver principalResolver) {
        this.erpBillingDataService = erpBillingDataService;
        this.erpBillingLineDao = erpBillingLineDao;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ErpBillingRunDTO> findErpBillingRuns(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            return toRuns(erpBillingLineDao.findRunSummaries(), erpBillingLineDao.findRunAccountCounts());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ErpBillingLineDTO> findErpBillingLines(Username authUsername, String billingPeriod,
                                                       String exportStatus) {
        try {
            principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            ExportStatus status = ApiValues.parseEnumOrNull(ExportStatus.class, exportStatus, "export status");
            List<ErpBillingLineDTO> result = new ArrayList<>();
            for (ErpBillingLine line : erpBillingDataService.findErpBillingLines(period(billingPeriod).getCode())) {
                if (status == null || line.getExportStatus() == status) {
                    result.add(toDTO(line));
                }
            }
            return result;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public List<ErpBillingLineDTO> previewErpBilling(Username authUsername, int month, int year) {
        try {
            principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            return toDTOs(erpBillingDataService.findAndCalculateErpBillingLines(month, year));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public List<ErpBillingLineDTO> runErpBilling(Username authUsername, int month, int year) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.BILLING_EDIT);
            List<ErpBillingLine> lines = erpBillingDataService.createErpBillingLines(month, year);
            LOG.info(String.format("ERP billing %s run by %s: %d lines", BillingPeriod.of(month, year),
                    principal.getUsername(), lines.size()));
            return toDTOs(lines);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public int retryErpExport(Username authUsername, String billingPeriod) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.BILLING_EDIT);
            int resent = erpBillingDataService.resendRefusedLines(period(billingPeriod).getCode());
            LOG.info(String.format("ERP lines of %s sent again by %s: %d", billingPeriod, principal.getUsername(),
                    resent));
            return resent;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsumptionRecordDTO> findRecordsWithoutExport(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            List<ConsumptionRecordDTO> result = new ArrayList<>();
            for (ConsumptionRecord record : erpBillingDataService.findRecordsWithoutExport()) {
                result.add(ConsumptionRecordApiService.toDTO(record));
            }
            return result;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private static BillingPeriod period(String billingPeriod) {
        if (billingPeriod == null || billingPeriod.isBlank()) {
            throw new InvalidInputException("Billing period is missing");
        }
        return BillingPeriod.parse(billingPeriod.trim());
    }

    /**
     * One run per period from the rows (period, export status, lines, amount, first creation) and the accounts per
     * period; periods newest first as the rows come.
     */
    static List<ErpBillingRunDTO> toRuns(List<Object[]> summaries, List<Object[]> accountCounts) {
        Map<String, Integer> accounts = new LinkedHashMap<>();
        for (Object[] row : accountCounts) {
            accounts.put((String) row[0], ((Number) row[1]).intValue());
        }
        Map<String, Map<String, Integer>> perStatus = new LinkedHashMap<>();
        Map<String, Integer> lines = new LinkedHashMap<>();
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        Map<String, LocalDateTime> runDates = new LinkedHashMap<>();
        for (Object[] row : summaries) {
            String period = (String) row[0];
            int count = ((Number) row[2]).intValue();
            perStatus.computeIfAbsent(period, p -> new LinkedHashMap<>()).put(row[1].toString(), count);
            lines.merge(period, count, Integer::sum);
            amounts.merge(period, (BigDecimal) row[3], BigDecimal::add);
            LocalDateTime created = (LocalDateTime) row[4];
            runDates.merge(period, created, (a, b) -> a.isBefore(b) ? a : b);
        }
        List<ErpBillingRunDTO> runs = new ArrayList<>();
        for (String period : perStatus.keySet()) {
            runs.add(new ErpBillingRunDTO(period, runDates.get(period), lines.get(period),
                    accounts.getOrDefault(period, 0), amounts.get(period), perStatus.get(period)));
        }
        return runs;
    }

    static List<ErpBillingLineDTO> toDTOs(List<ErpBillingLine> lines) {
        List<ErpBillingLineDTO> result = new ArrayList<>(lines.size());
        for (ErpBillingLine line : lines) {
            result.add(toDTO(line));
        }
        return result;
    }

    static ErpBillingLineDTO toDTO(ErpBillingLine line) {
        return new ErpBillingLineDTO(line.getId(), line.getBillingPeriod(), line.getBillingAccountNo(),
                line.getErpPartnerNo(), line.getOrderNumber(), line.getLineType().name(), line.getArticleNo(),
                line.getDescription(), line.getQuantity(), line.getUnitPrice(), line.getAmount(), line.getCurrency(),
                line.getExportStatus().name(), line.getErrorText(), line.getCreateDate(), line.getExportDate(),
                line.getAnswerDate());
    }
}
